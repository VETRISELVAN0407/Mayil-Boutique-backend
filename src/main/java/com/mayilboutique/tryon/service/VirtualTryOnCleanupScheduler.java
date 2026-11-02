package com.mayilboutique.tryon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically deletes expired try-on photos and results. A failed run is logged and retried on the next tick. */
@Component
public class VirtualTryOnCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(VirtualTryOnCleanupScheduler.class);

    private final TryOnCleanupService cleanup;

    public VirtualTryOnCleanupScheduler(TryOnCleanupService cleanup) {
        this.cleanup = cleanup;
    }

    @Scheduled(fixedDelayString = "${app.tryon.cleanup-interval-ms:300000}", initialDelayString = "${app.tryon.cleanup-initial-delay-ms:30000}")
    public void run() {
        try {
            cleanup.cleanup();
        } catch (RuntimeException ex) {
            log.error("Try-on cleanup failed ({})", ex.getClass().getSimpleName());
        }
    }
}
