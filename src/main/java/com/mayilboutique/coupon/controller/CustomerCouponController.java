package com.mayilboutique.coupon.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.core.security.CustomUserDetails;
import com.mayilboutique.coupon.dto.ApplyCouponRequest;
import com.mayilboutique.coupon.dto.CouponQuoteResponse;
import com.mayilboutique.coupon.service.CouponService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customer/coupons")
public class CustomerCouponController {

    private final CouponService couponService;

    public CustomerCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/validate")
    public ResponseEntity<CouponQuoteResponse> validate(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @Valid @RequestBody ApplyCouponRequest request) {
        return ResponseEntity.ok(couponService.quote(principal.getUserId(), request.getCode()));
    }
}
