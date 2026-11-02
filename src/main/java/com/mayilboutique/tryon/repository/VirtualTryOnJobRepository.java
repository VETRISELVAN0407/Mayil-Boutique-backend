package com.mayilboutique.tryon.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mayilboutique.tryon.JobStatus;
import com.mayilboutique.tryon.entity.VirtualTryOnJob;

public interface VirtualTryOnJobRepository extends JpaRepository<VirtualTryOnJob, Long> {

    Optional<VirtualTryOnJob> findByIdAndCustomerId(Long id, Long customerId);

    long countByCustomerIdAndCreatedAtAfter(Long customerId, Instant since);

    boolean existsByCustomerIdAndStatusInAndExpiresAtAfter(Long customerId, Collection<JobStatus> statuses, Instant now);

    /** Jobs past their expiry that still hold files or are still marked as running. */
    @Query("SELECT j FROM VirtualTryOnJob j WHERE j.expiresAt < :now AND (j.status IN :active "
            + "OR j.originalPhotoPath IS NOT NULL OR j.generatedImagePath IS NOT NULL) ORDER BY j.expiresAt")
    List<VirtualTryOnJob> findDueForCleanup(@Param("now") Instant now,
                                            @Param("active") Collection<JobStatus> active, Pageable page);

    /** Oldest first; the worker's queue. Expired jobs are left to the cleanup run. */
    List<VirtualTryOnJob> findByStatusAndExpiresAtAfterOrderByCreatedAtAsc(JobStatus status, Instant now, Pageable page);

    // ---- admin statistics (rows outlive their files, so history stays available after photos are deleted) ----

    long countByCreatedAtAfter(Instant since);

    long countByStatusAndCreatedAtAfter(JobStatus status, Instant since);

    /** Generation finished OK. Such a job may later read EXPIRED once its files are deleted, so status alone is not enough. */
    @Query("SELECT COUNT(j) FROM VirtualTryOnJob j WHERE j.createdAt >= :since AND j.completedAt IS NOT NULL AND j.status <> :failed")
    long countSucceededSince(@Param("since") Instant since, @Param("failed") JobStatus failed);

    /** Jobs that never finished: still running, or cancelled/expired before completing. */
    @Query("SELECT COUNT(j) FROM VirtualTryOnJob j WHERE j.createdAt >= :since AND j.completedAt IS NULL AND j.status IN :statuses")
    long countUnfinishedSince(@Param("since") Instant since, @Param("statuses") Collection<JobStatus> statuses);

    @Query("SELECT COUNT(DISTINCT j.customerId) FROM VirtualTryOnJob j WHERE j.createdAt >= :since")
    long countCustomersSince(@Param("since") Instant since);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (completed_at - created_at))) FROM virtual_try_on_jobs "
            + "WHERE created_at >= :since AND completed_at IS NOT NULL AND status <> 'FAILED'", nativeQuery = true)
    Double averageSecondsSince(@Param("since") Instant since);

    @Query("SELECT j.garmentType, COUNT(j) FROM VirtualTryOnJob j WHERE j.createdAt >= :since GROUP BY j.garmentType ORDER BY COUNT(j) DESC")
    List<Object[]> countByGarmentTypeSince(@Param("since") Instant since);

    @Query("SELECT j.productName, COUNT(j) FROM VirtualTryOnJob j WHERE j.createdAt >= :since GROUP BY j.productName ORDER BY COUNT(j) DESC, j.productName")
    List<Object[]> topProductsSince(@Param("since") Instant since, Pageable page);

    boolean existsByOriginalPhotoPathOrGeneratedImagePath(String original, String generated);
}
