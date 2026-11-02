package com.mayilboutique.tryon.dto;

import java.time.Instant;

import com.mayilboutique.tryon.JobStatus;

/** What the customer sees of a job. Never includes file names or provider details. */
public record TryOnJobResponse(Long id, Long productId, String productName, JobStatus status, String errorMessage,
                               boolean resultAvailable, Instant createdAt, Instant completedAt, Instant expiresAt) {
}
