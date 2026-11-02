package com.mayilboutique.tryon.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.tryon.JobStatus;
import com.mayilboutique.tryon.TryOnProperties;
import com.mayilboutique.tryon.dto.TryOnJobResponse;
import com.mayilboutique.tryon.dto.TryOnProductResponse;
import com.mayilboutique.tryon.entity.VirtualTryOnJob;
import com.mayilboutique.tryon.provider.ProviderAssets;
import com.mayilboutique.tryon.repository.VirtualTryOnJobRepository;

/**
 * Customer-facing job lifecycle. Every lookup is scoped to the signed-in customer, so another customer's job is
 * simply "not found". Files are temporary: they are deleted when {@code expiresAt} passes (see the cleanup
 * scheduler) or when the customer deletes the job.
 */
@Service
public class TryOnJobService {

    private static final Logger log = LoggerFactory.getLogger(TryOnJobService.class);

    private final VirtualTryOnJobRepository jobs;
    private final TryOnConfigService configService;
    private final TryOnFileStorage storage;
    private final TryOnProperties props;
    private final Clock clock;
    private final ProviderAssets providerAssets;

    public TryOnJobService(VirtualTryOnJobRepository jobs, TryOnConfigService configService,
                           TryOnFileStorage storage, TryOnProperties props, Clock clock,
                           ProviderAssets providerAssets) {
        this.jobs = jobs;
        this.configService = configService;
        this.storage = storage;
        this.props = props;
        this.clock = clock;
        this.providerAssets = providerAssets;
    }

    /** A readable result file together with its content type. */
    public record ResultFile(Path path, org.springframework.http.MediaType mediaType) {
    }

    @Transactional
    public TryOnJobResponse create(Long customerId, Long productId, MultipartFile photo, boolean consent) {
        if (!props.isEnabled()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Virtual Try-On is not available yet");
        }
        if (!consent) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Please accept the photo processing consent to continue");
        }
        TryOnProductResponse product = configService.getEligible(productId);

        Instant now = clock.instant();
        if (jobs.existsByCustomerIdAndStatusInAndExpiresAtAfter(customerId, EnumSet.of(JobStatus.PENDING,
                JobStatus.PROCESSING), now)) {
            throw new BusinessException(HttpStatus.CONFLICT, "A preview is already being generated. Please wait for it");
        }
        if (jobs.countByCustomerIdAndCreatedAtAfter(customerId, now.minus(Duration.ofHours(1))) >= props.getMaxJobsPerHour()
                || jobs.countByCustomerIdAndCreatedAtAfter(customerId, now.minus(Duration.ofDays(1))) >= props.getMaxJobsPerDay()) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "You have reached the try-on limit for now. Please try again later");
        }

        String stored = storage.storePhoto(photo);
        try {
            VirtualTryOnJob job = new VirtualTryOnJob(customerId, productId, product.product().getName(),
                    product.garmentType(), stored, now, now.plus(Duration.ofMinutes(props.getActiveJobMinutes())));
            return toResponse(jobs.save(job), now);
        } catch (RuntimeException ex) {
            storage.delete(stored);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public TryOnJobResponse get(Long customerId, Long jobId) {
        return toResponse(owned(customerId, jobId), clock.instant());
    }

    /** The generated image, only while the job is completed and not expired. */
    @Transactional(readOnly = true)
    public ResultFile result(Long customerId, Long jobId) {
        VirtualTryOnJob job = owned(customerId, jobId);
        Instant now = clock.instant();
        JobStatus status = effectiveStatus(job, now);
        if (status == JobStatus.EXPIRED) {
            throw new BusinessException(HttpStatus.GONE, "This result has expired");
        }
        if (status != JobStatus.COMPLETED) {
            throw new BusinessException(HttpStatus.CONFLICT, "The result is not ready");
        }
        Path path = storage.find(job.getGeneratedImagePath())
                .orElseThrow(() -> new BusinessException(HttpStatus.GONE, "This result has expired"));
        return new ResultFile(path, storage.mediaTypeFor(path.getFileName().toString()));
    }

    /** Deletes the uploaded photo and any result right away. Safe to call repeatedly. */
    @Transactional
    public void delete(Long customerId, Long jobId) {
        VirtualTryOnJob job = owned(customerId, jobId);
        // A running job is cancelled; a finished one keeps its row but loses its pictures.
        if (job.getStatus().isActive()) {
            job.setStatus(JobStatus.CANCELLED);
        } else if (job.getStatus() == JobStatus.COMPLETED) {
            job.setStatus(JobStatus.EXPIRED);
        }
        purgeFiles(job);
        providerAssets.deleteQuietly(job.getProviderJobId());
        job.setExpiresAt(clock.instant());
        jobs.save(job);
    }

    /** The worker handed the job to the provider. */
    @Transactional
    public void markProcessing(Long jobId, String provider, String providerJobId) {
        VirtualTryOnJob job = jobs.findById(jobId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Job not found"));
        if (job.getStatus() != JobStatus.PENDING) {
            providerAssets.deleteQuietly(providerJobId); // cancelled meanwhile: drop what was just sent
            return;
        }
        job.setStatus(JobStatus.PROCESSING);
        job.setProvider(provider);
        job.setProviderJobId(providerJobId);
        jobs.save(job);
    }

    /** Counts a failed start attempt and returns the new total. */
    @Transactional
    public int recordFailedAttempt(Long jobId) {
        VirtualTryOnJob job = jobs.findById(jobId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Job not found"));
        job.setAttempts(job.getAttempts() + 1);
        return jobs.save(job).getAttempts();
    }

    /** Called by the generation worker (Phase 3) when the provider returned an image. */
    @Transactional
    public void complete(Long jobId, byte[] resultImage) {
        VirtualTryOnJob job = jobs.findById(jobId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Job not found"));
        if (!job.getStatus().isActive()) {
            return; // cancelled or expired while the provider was working: drop the result
        }
        Instant now = clock.instant();
        job.setGeneratedImagePath(storage.storeResult(resultImage));
        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(now);
        job.setExpiresAt(now.plus(Duration.ofMinutes(props.getRetentionMinutes())));
        jobs.save(job);
    }

    /** Called by the worker on failure. {@code safeMessage} is shown to the customer, so it must not be provider output. */
    @Transactional
    public void fail(Long jobId, String safeMessage) {
        VirtualTryOnJob job = jobs.findById(jobId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Job not found"));
        if (!job.getStatus().isActive()) {
            return;
        }
        Instant now = clock.instant();
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(safeMessage);
        job.setCompletedAt(now);
        job.setExpiresAt(now.plus(Duration.ofMinutes(props.getRetentionMinutes())));
        jobs.save(job);
    }

    private VirtualTryOnJob owned(Long customerId, Long jobId) {
        return jobs.findByIdAndCustomerId(jobId, customerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Try-on job not found"));
    }

    void purgeFiles(VirtualTryOnJob job) {
        storage.delete(job.getOriginalPhotoPath());
        storage.delete(job.getGeneratedImagePath());
        job.setOriginalPhotoPath(null);
        job.setGeneratedImagePath(null);
    }

    /** Past its expiry a job reads as EXPIRED even before the cleanup run has caught up with it. */
    static JobStatus effectiveStatus(VirtualTryOnJob job, Instant now) {
        boolean pastExpiry = !job.getExpiresAt().isAfter(now);
        if (pastExpiry && (job.getStatus().isActive() || job.getStatus() == JobStatus.COMPLETED)) {
            return JobStatus.EXPIRED;
        }
        return job.getStatus();
    }

    private TryOnJobResponse toResponse(VirtualTryOnJob job, Instant now) {
        JobStatus status = effectiveStatus(job, now);
        boolean available = status == JobStatus.COMPLETED && job.getGeneratedImagePath() != null;
        return new TryOnJobResponse(job.getId(), job.getProductId(), job.getProductName(), status,
                job.getErrorMessage(), available, job.getCreatedAt(), job.getCompletedAt(), job.getExpiresAt());
    }
}
