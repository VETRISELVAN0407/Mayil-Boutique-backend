package com.mayilboutique.coupon.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mayilboutique.coupon.entity.CouponRedemption;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);

    boolean existsByCouponId(Long couponId);

    Optional<CouponRedemption> findByOrderId(Long orderId);
}
