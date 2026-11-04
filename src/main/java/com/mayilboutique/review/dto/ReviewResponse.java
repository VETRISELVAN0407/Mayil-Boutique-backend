package com.mayilboutique.review.dto;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        int rating,
        String comment,
        String reviewerName,
        String colour,
        String size,
        Instant createdAt,
        long helpfulCount,
        boolean helpfulByMe,
        boolean mine) {
}
