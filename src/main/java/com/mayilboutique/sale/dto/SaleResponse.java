package com.mayilboutique.sale.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.mayilboutique.sale.entity.Sale;

/** {@code state} is derived from the dates and the active flag: SCHEDULED, RUNNING, ENDED or PAUSED. */
public record SaleResponse(Long id, String name, BigDecimal percentOff, boolean allProducts, Long categoryId,
                           List<Long> productIds, Instant startsAt, Instant endsAt, boolean active, String state) {

    public static SaleResponse from(Sale s, Instant now) {
        String state;
        if (!s.isActive()) {
            state = "PAUSED";
        } else if (now.isBefore(s.getStartsAt())) {
            state = "SCHEDULED";
        } else if (now.isBefore(s.getEndsAt())) {
            state = "RUNNING";
        } else {
            state = "ENDED";
        }
        return new SaleResponse(s.getId(), s.getName(), s.getPercentOff(), s.isAllProducts(), s.getCategoryId(),
                s.getProductIds().stream().sorted().toList(), s.getStartsAt(), s.getEndsAt(), s.isActive(), state);
    }
}
