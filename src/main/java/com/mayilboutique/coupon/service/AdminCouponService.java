package com.mayilboutique.coupon.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.coupon.dto.CouponRequest;
import com.mayilboutique.coupon.dto.CouponResponse;
import com.mayilboutique.coupon.entity.Coupon;
import com.mayilboutique.coupon.entity.CouponType;
import com.mayilboutique.coupon.repository.CouponRedemptionRepository;
import com.mayilboutique.coupon.repository.CouponRepository;

@Service
public class AdminCouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;

    public AdminCouponService(CouponRepository couponRepository, CouponRedemptionRepository redemptionRepository) {
        this.couponRepository = couponRepository;
        this.redemptionRepository = redemptionRepository;
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> list() {
        return couponRepository.findAllByOrderByCreatedAtDescIdDesc().stream().map(CouponResponse::from).toList();
    }

    @Transactional
    public CouponResponse create(CouponRequest request) {
        String code = CouponService.normalize(request.getCode());
        if (couponRepository.existsByCode(code)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Coupon code already exists");
        }
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        apply(coupon, request);
        coupon.setActive(request.getActive() == null || request.getActive());
        return CouponResponse.from(couponRepository.save(coupon));
    }

    /** The code and its use count never change; everything else can. */
    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = find(id);
        if (!coupon.getCode().equals(CouponService.normalize(request.getCode()))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "The coupon code cannot be changed");
        }
        apply(coupon, request);
        if (request.getActive() != null) {
            coupon.setActive(request.getActive());
        }
        if (coupon.getUsageLimit() != null && coupon.getUsageLimit() < coupon.getUsedCount()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Usage limit cannot be lower than the " + coupon.getUsedCount() + " times it was already used");
        }
        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse setActive(Long id, boolean active) {
        Coupon coupon = find(id);
        coupon.setActive(active);
        return CouponResponse.from(couponRepository.save(coupon));
    }

    /** A coupon that was ever used stays (orders keep its code); deactivate it instead. */
    @Transactional
    public void delete(Long id) {
        Coupon coupon = find(id);
        if (coupon.getUsedCount() > 0 || redemptionRepository.existsByCouponId(id)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "This coupon has been used on orders. Deactivate it instead of deleting it.");
        }
        couponRepository.delete(coupon);
    }

    private void apply(Coupon coupon, CouponRequest r) {
        BigDecimal value = r.getValue() == null ? BigDecimal.ZERO : r.getValue();
        switch (r.getType()) {
            case PERCENT -> {
                if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "Percentage must be between 0 and 100");
                }
            }
            case FLAT -> {
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "Flat discount must be greater than zero");
                }
            }
            case FREE_SHIPPING -> value = BigDecimal.ZERO;
        }
        if (r.getStartsAt() != null && r.getEndsAt() != null && !r.getEndsAt().isAfter(r.getStartsAt())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "End date must be after the start date");
        }
        coupon.setDescription(r.getDescription() == null || r.getDescription().isBlank() ? null : r.getDescription().trim());
        coupon.setType(r.getType());
        coupon.setValue(value);
        coupon.setMaxDiscount(r.getType() == CouponType.PERCENT ? r.getMaxDiscount() : null);
        coupon.setMinOrderValue(r.getMinOrderValue() == null ? BigDecimal.ZERO : r.getMinOrderValue());
        coupon.setStartsAt(r.getStartsAt());
        coupon.setEndsAt(r.getEndsAt());
        coupon.setUsageLimit(r.getUsageLimit());
        coupon.setOncePerCustomer(r.isOncePerCustomer());
    }

    private Coupon find(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Coupon not found"));
    }
}
