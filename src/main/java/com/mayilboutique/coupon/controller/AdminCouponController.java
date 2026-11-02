package com.mayilboutique.coupon.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.coupon.dto.CouponRequest;
import com.mayilboutique.coupon.dto.CouponResponse;
import com.mayilboutique.coupon.service.AdminCouponService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final AdminCouponService service;

    public AdminCouponController(AdminCouponService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CouponResponse> update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    /** Body: {@code {"active": true|false}}. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<CouponResponse> setStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        if (active == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "active is required");
        }
        return ResponseEntity.ok(service.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
