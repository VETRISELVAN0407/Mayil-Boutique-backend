package com.mayilboutique.admin.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.mayilboutique.core.enums.UserStatus;

/** totalSpent excludes cancelled and refunded orders, matching the dashboard sales metric. */
public record CustomerSummaryResponse(
        Long id,
        String mobile,
        UserStatus status,
        Instant joinedAt,
        long orderCount,
        BigDecimal totalSpent,
        Instant lastOrderAt) {
}
