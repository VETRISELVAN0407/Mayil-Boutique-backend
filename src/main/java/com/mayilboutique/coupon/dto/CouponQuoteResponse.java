package com.mayilboutique.coupon.dto;

import java.math.BigDecimal;

import com.mayilboutique.coupon.entity.CouponType;

/** What a coupon would take off the customer's current cart. */
public record CouponQuoteResponse(String code, CouponType type, String description, BigDecimal subtotal,
                                  BigDecimal discount, BigDecimal total) {
}
