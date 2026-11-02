package com.mayilboutique.tryon.service;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.service.ProductImageStorageService;
import com.mayilboutique.tryon.JobStatus;
import com.mayilboutique.tryon.TryOnProperties;
import com.mayilboutique.tryon.entity.VirtualTryOnJob;
import com.mayilboutique.tryon.provider.ProviderAssets;
import com.mayilboutique.tryon.provider.ProviderException;
import com.mayilboutique.tryon.provider.ProviderStatus;
import com.mayilboutique.tryon.provider.TryOnRequest;
import com.mayilboutique.tryon.provider.VirtualTryOnProvider;
import com.mayilboutique.tryon.repository.VirtualTryOnJobRepository;

/**
 * Moves jobs through the provider: PENDING jobs are submitted, PROCESSING jobs are polled. State lives in the
 * database, so a restart loses nothing. Written for a single backend instance (two instances could submit the same
 * pending job twice); revisit with row locking before scaling out.
 */
@Component
public class TryOnJobWorker {

    private static final Logger log = LoggerFactory.getLogger(TryOnJobWorker.class);

    static final String FAILURE_MESSAGE =
            "We could not create your preview. Please try again with a clear, full-body photo.";
    private static final int BATCH = 5;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(5);

    private final ObjectProvider<VirtualTryOnProvider> providers;
    private final VirtualTryOnJobRepository jobs;
    private final TryOnJobService jobService;
    private final TryOnConfigService configService;
    private final TryOnFileStorage storage;
    private final ProductImageStorageService productImages;
    private final ProviderAssets providerAssets;
    private final TryOnProperties props;
    private final Clock clock;
    private final Map<Long, Instant> retryNotBefore = new ConcurrentHashMap<>();

    public TryOnJobWorker(ObjectProvider<VirtualTryOnProvider> providers, VirtualTryOnJobRepository jobs,
                          TryOnJobService jobService, TryOnConfigService configService, TryOnFileStorage storage,
                          ProductImageStorageService productImages, ProviderAssets providerAssets,
                          TryOnProperties props, Clock clock) {
        this.providers = providers;
        this.jobs = jobs;
        this.jobService = jobService;
        this.configService = configService;
        this.storage = storage;
        this.productImages = productImages;
        this.providerAssets = providerAssets;
        this.props = props;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.tryon.worker-interval-ms:2000}", initialDelayString = "${app.tryon.worker-initial-delay-ms:10000}")
    public void run() {
        try {
            runOnce();
        } catch (RuntimeException ex) {
            log.error("Try-on worker run failed ({})", ex.getClass().getSimpleName());
        }
    }

    /** One pass; public so it can be driven directly in tests. */
    public void runOnce() {
        VirtualTryOnProvider provider = providers.getIfAvailable();
        if (provider == null || !props.isEnabled()) {
            return;
        }
        Instant now = clock.instant();
        for (VirtualTryOnJob job : jobs.findByStatusAndExpiresAtAfterOrderByCreatedAtAsc(JobStatus.PENDING, now, PageRequest.of(0, BATCH))) {
            submit(provider, job, now);
        }
        for (VirtualTryOnJob job : jobs.findByStatusAndExpiresAtAfterOrderByCreatedAtAsc(JobStatus.PROCESSING, now, PageRequest.of(0, BATCH * 4))) {
            poll(provider, job);
        }
    }

    private void submit(VirtualTryOnProvider provider, VirtualTryOnJob job, Instant now) {
        Long id = job.getId();
        if (retryNotBefore.getOrDefault(id, Instant.MIN).isAfter(now)) {
            return;
        }
        TryOnRequest request;
        try {
            request = buildRequest(job);
        } catch (IOException | RuntimeException ex) {
            log.warn("Try-on job {} cannot be submitted ({})", id, ex.getClass().getSimpleName());
            jobService.fail(id, FAILURE_MESSAGE);
            return;
        }
        try {
            String providerJobId = provider.start(request);
            retryNotBefore.remove(id);
            jobService.markProcessing(id, provider.name(), providerJobId);
        } catch (ProviderException ex) {
            if (!ex.isRetryable()) {
                log.warn("Try-on job {} rejected by provider: {}", id, ex.getMessage());
                jobService.fail(id, FAILURE_MESSAGE);
                return;
            }
            int attempts = jobService.recordFailedAttempt(id);
            if (attempts >= props.getMaxStartAttempts()) {
                log.warn("Try-on job {} failed to start after {} attempts: {}", id, attempts, ex.getMessage());
                retryNotBefore.remove(id);
                jobService.fail(id, FAILURE_MESSAGE);
            } else {
                retryNotBefore.put(id, now.plus(RETRY_DELAY));
            }
        }
    }

    private void poll(VirtualTryOnProvider provider, VirtualTryOnJob job) {
        Long id = job.getId();
        String providerJobId = job.getProviderJobId();
        if (providerJobId == null) {
            jobService.fail(id, FAILURE_MESSAGE);
            return;
        }
        try {
            ProviderStatus status = provider.status(providerJobId);
            switch (status.state()) {
                case DONE -> {
                    byte[] image = provider.result(providerJobId);
                    jobService.complete(id, image);
                    providerAssets.deleteQuietly(providerJobId);
                }
                case FAILED -> {
                    jobService.fail(id, FAILURE_MESSAGE);
                    providerAssets.deleteQuietly(providerJobId);
                }
                default -> { /* still queued or running */ }
            }
        } catch (ProviderException ex) {
            if (!ex.isRetryable()) {
                log.warn("Try-on job {} lost at provider: {}", id, ex.getMessage());
                jobService.fail(id, FAILURE_MESSAGE);
                providerAssets.deleteQuietly(providerJobId);
            } // transient: try again on the next pass; the job's own expiry bounds the wait
        } catch (BusinessException ex) {
            // The provider returned something we cannot store as an image.
            jobService.fail(id, FAILURE_MESSAGE);
            providerAssets.deleteQuietly(providerJobId);
        }
    }

    private TryOnRequest buildRequest(VirtualTryOnJob job) throws IOException {
        if (job.getProductId() == null) {
            throw new BusinessException(org.springframework.http.HttpStatus.NOT_FOUND, "Product removed");
        }
        byte[] person = Files.readAllBytes(storage.find(job.getOriginalPhotoPath())
                .orElseThrow(() -> new BusinessException(org.springframework.http.HttpStatus.GONE, "Photo gone")));
        List<byte[]> garments = new ArrayList<>();
        for (String filename : configService.garmentImageFilenames(job.getProductId())) {
            garments.add(Files.readAllBytes(productImages.resolve(filename)));
        }
        return new TryOnRequest(job.getGarmentType(), person, garments);
    }
}
