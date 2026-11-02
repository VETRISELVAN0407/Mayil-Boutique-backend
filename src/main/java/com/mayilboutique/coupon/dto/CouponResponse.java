package com.mayilboutique.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mayilboutique.coupon.entity.Coupon;
import com.mayilboutique.coupon.entity.CouponType;

public record CouponResponse(
        Long id, String code, String description, CouponType type, BigDecimal value, BigDecimal maxDiscount,
        BigDecimal minOrderValue, Instant startsAt, Instant endsAt, Integer usageLimit, int usedCount,
        boolean oncePerCustomer, boolean active, Instant createdAt) {

    public static CouponResponse from(Coupon c) {
        return new CouponResponse(c.getId(), c.getCode(), c.getDescription(), c.getType(), c.getValue(),
                c.getMaxDiscount(), c.getMinOrderValue(), c.getStartsAt(), c.getEndsAt(), c.getUsageLimit(),
                c.getUsedCount(), c.isOncePerCustomer(), c.isActive(), c.getCreatedAt());
    }
}
