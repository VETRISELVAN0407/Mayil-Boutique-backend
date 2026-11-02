package com.mayilboutique.coupon.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "coupon_redemptions")
public class CouponRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false, updatable = false)
    private Long couponId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CouponRedemption() {
    }

    public CouponRedemption(Long couponId, Long userId, Long orderId, BigDecimal discount) {
        this.couponId = couponId;
        this.userId = userId;
        this.orderId = orderId;
        this.discount = discount;
    }

    public Long getId() { return id; }
    public Long getCouponId() { return couponId; }
    public Long getUserId() { return userId; }
    public Long getOrderId() { return orderId; }
    public BigDecimal getDiscount() { return discount; }
    public Instant getCreatedAt() { return createdAt; }
}
