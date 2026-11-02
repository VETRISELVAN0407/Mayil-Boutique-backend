package com.mayilboutique.coupon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.mayilboutique.cart.repository.CartItemRepository;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.coupon.entity.Coupon;
import com.mayilboutique.coupon.entity.CouponRedemption;
import com.mayilboutique.coupon.entity.CouponType;
import com.mayilboutique.coupon.repository.CouponRedemptionRepository;
import com.mayilboutique.coupon.repository.CouponRepository;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.repository.ProductVariantRepository;
import com.mayilboutique.sale.service.SalePricingService;

class CouponServiceTest {

    private static final Long USER = 7L;

    private CouponRepository couponRepository;
    private CouponRedemptionRepository redemptionRepository;
    private CouponService service;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        couponRepository = mock(CouponRepository.class);
        redemptionRepository = mock(CouponRedemptionRepository.class);
        service = new CouponService(couponRepository, redemptionRepository, mock(CartItemRepository.class),
                mock(ProductVariantRepository.class), mock(ProductRepository.class), mock(SalePricingService.class));
    }

    private Coupon coupon(CouponType type, String value) {
        Coupon c = new Coupon();
        ReflectionTestUtils.setField(c, "id", 1L);
        c.setCode("SAVE");
        c.setType(type);
        c.setValue(new BigDecimal(value));
        return c;
    }

    private static BigDecimal money(String v) {
        return new BigDecimal(v);
    }

    @Test
    void percentCouponTakesThatShareOfTheSubtotal() {
        assertEquals(money("150.00"), service.evaluate(coupon(CouponType.PERCENT, "10"), USER, money("1500"), now).discount());
    }

    @Test
    void percentCouponIsCappedByMaxDiscount() {
        Coupon c = coupon(CouponType.PERCENT, "50");
        c.setMaxDiscount(money("200"));
        assertEquals(money("200.00"), service.evaluate(c, USER, money("1500"), now).discount());
    }

    @Test
    void flatCouponNeverExceedsTheSubtotal() {
        assertEquals(money("300.00"), service.evaluate(coupon(CouponType.FLAT, "500"), USER, money("300"), now).discount());
        assertEquals(money("500.00"), service.evaluate(coupon(CouponType.FLAT, "500"), USER, money("900"), now).discount());
    }

    @Test
    void freeShippingTakesNothingOffWhileShippingIsFree() {
        assertEquals(money("0.00"), service.evaluate(coupon(CouponType.FREE_SHIPPING, "0"), USER, money("900"), now).discount());
    }

    @Test
    void belowMinimumOrderValueIsRejected() {
        Coupon c = coupon(CouponType.FLAT, "100");
        c.setMinOrderValue(money("1000"));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.evaluate(c, USER, money("999"), now));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void inactiveNotYetStartedAndExpiredCouponsAreRejected() {
        Coupon inactive = coupon(CouponType.FLAT, "100");
        inactive.setActive(false);
        Coupon future = coupon(CouponType.FLAT, "100");
        future.setStartsAt(now.plus(Duration.ofDays(1)));
        Coupon expired = coupon(CouponType.FLAT, "100");
        expired.setEndsAt(now.minus(Duration.ofSeconds(1)));
        for (Coupon c : new Coupon[] {inactive, future, expired}) {
            assertThrows(BusinessException.class, () -> service.evaluate(c, USER, money("1000"), now));
        }
    }

    @Test
    void usageLimitStopsFurtherUse() {
        Coupon c = coupon(CouponType.FLAT, "100");
        c.setUsageLimit(2);
        c.setUsedCount(2);
        assertThrows(BusinessException.class, () -> service.evaluate(c, USER, money("1000"), now));
    }

    @Test
    void oncePerCustomerBlocksASecondUseButNotAnotherCustomer() {
        Coupon c = coupon(CouponType.FLAT, "100");
        c.setOncePerCustomer(true);
        when(redemptionRepository.existsByCouponIdAndUserId(1L, USER)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.evaluate(c, USER, money("1000"), now));
        assertEquals(money("100.00"), service.evaluate(c, USER + 1, money("1000"), now).discount());
    }

    @Test
    void unknownCodeIsRejectedWithAGenericMessage() {
        when(couponRepository.findByCodeForUpdate("NOPE")).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.lockAndEvaluate(USER, " nope ", money("1000")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void releasingAnOrderGivesTheUseBack() {
        Coupon c = coupon(CouponType.FLAT, "100");
        c.setUsedCount(3);
        CouponRedemption redemption = new CouponRedemption(1L, USER, 55L, money("100"));
        when(redemptionRepository.findByOrderId(55L)).thenReturn(Optional.of(redemption));
        when(couponRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(c));

        service.release(55L);

        assertEquals(2, c.getUsedCount());
        verify(redemptionRepository).delete(redemption);
    }
}
