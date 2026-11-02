package com.mayilboutique.tryon.dto;

import java.math.BigDecimal;
import java.util.List;

import com.mayilboutique.tryon.GarmentType;

/**
 * Usage numbers for the admin. {@code estimatedCost} is null unless a per-generation cost is configured: a
 * self-hosted engine reports no usage cost of its own. {@code successRate} counts finished jobs only
 * (succeeded / (succeeded + failed)).
 */
public record TryOnStatsResponse(int periodDays, boolean featureEnabled, boolean providerConfigured,
                                 List<GarmentType> supportedGarmentTypes,
                                 long total, long succeeded, long failed, long abandoned, long inProgress,
                                 long uniqueCustomers, Double averageSeconds, Double successRate,
                                 BigDecimal estimatedCost, List<Count> byGarmentType, List<Count> topProducts) {

    public record Count(String label, long count) {
    }
}
