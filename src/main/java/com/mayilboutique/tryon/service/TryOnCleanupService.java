package com.mayilboutique.tryon.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.tryon.JobStatus;
import com.mayilboutique.tryon.TryOnProperties;
import com.mayilboutique.tryon.entity.VirtualTryOnJob;
import com.mayilboutique.tryon.provider.ProviderAssets;
import com.mayilboutique.tryon.repository.VirtualTryOnJobRepository;

/**
 * Deletes expired customer files. Safe to run repeatedly and from several instances: missing files are ignored and
 * a job that is already clean is not picked up again.
 */
@Service
public class TryOnCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TryOnCleanupService.class);
    private static final int BATCH = 100;

    private final VirtualTryOnJobRepository jobs;
    private final TryOnFileStorage storage;
    private final TryOnProperties props;
    private final Clock clock;
    private final ProviderAssets providerAssets;

    public TryOnCleanupService(VirtualTryOnJobRepository jobs, TryOnFileStorage storage, TryOnProperties props,
                               Clock clock, ProviderAssets providerAssets) {
        this.jobs = jobs;
        this.storage = storage;
        this.props = props;
        this.clock = clock;
        this.providerAssets = providerAssets;
    }

    /** Returns how many jobs and stray files were cleaned. */
    @Transactional
    public int cleanup() {
        Instant now = clock.instant();
        int cleaned = 0;

        List<VirtualTryOnJob> due;
        do {
            due = jobs.findDueForCleanup(now, EnumSet.of(JobStatus.PENDING, JobStatus.PROCESSING), PageRequest.of(0, BATCH));
            for (VirtualTryOnJob job : due) {
                storage.delete(job.getOriginalPhotoPath());
                storage.delete(job.getGeneratedImagePath());
                providerAssets.deleteQuietly(job.getProviderJobId());
                job.setOriginalPhotoPath(null);
                job.setGeneratedImagePath(null);
                // Unfinished or finished-but-not-collected jobs become EXPIRED; FAILED/CANCELLED/EXPIRED keep their status.
                if (job.getStatus().isActive() || job.getStatus() == JobStatus.COMPLETED) {
                    job.setStatus(JobStatus.EXPIRED);
                }
                jobs.save(job);
                cleaned++;
            }
            jobs.flush();
        } while (due.size() == BATCH);

        cleaned += sweepStrayFiles(now);
        if (cleaned > 0) {
            log.info("Try-on cleanup removed files for {} item(s)", cleaned);
        }
        return cleaned;
    }

    /** Files no job refers to (e.g. an upload whose transaction failed hard) are removed once clearly old. */
    private int sweepStrayFiles(Instant now) {
        Duration grace = Duration.ofMinutes(Math.max(props.getRetentionMinutes(), props.getActiveJobMinutes()) * 2);
        int removed = 0;
        for (String name : storage.listOlderThan(now.minus(grace))) {
            if (!jobs.existsByOriginalPhotoPathOrGeneratedImagePath(name, name)) {
                storage.delete(name);
                removed++;
            }
        }
        return removed;
    }
}
