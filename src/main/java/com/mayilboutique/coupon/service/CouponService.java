package com.mayilboutique.coupon.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.cart.entity.CartItem;
import com.mayilboutique.cart.repository.CartItemRepository;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.coupon.dto.CouponQuoteResponse;
import com.mayilboutique.coupon.entity.Coupon;
import com.mayilboutique.coupon.entity.CouponRedemption;
import com.mayilboutique.coupon.repository.CouponRedemptionRepository;
import com.mayilboutique.coupon.repository.CouponRepository;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductVariant;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.repository.ProductVariantRepository;
import com.mayilboutique.sale.service.SalePricingService;

/** Customer-side coupon rules: checking a code against the cart and redeeming it when an order is placed. */
@Service
public class CouponService {

    /** A validated coupon and what it takes off. */
    public record Applied(Coupon coupon, BigDecimal discount) {
    }

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final SalePricingService pricing;

    public CouponService(CouponRepository couponRepository, CouponRedemptionRepository redemptionRepository,
                         CartItemRepository cartItemRepository, ProductVariantRepository variantRepository,
                         ProductRepository productRepository, SalePricingService pricing) {
        this.couponRepository = couponRepository;
        this.redemptionRepository = redemptionRepository;
        this.cartItemRepository = cartItemRepository;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.pricing = pricing;
    }

    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    /** Checks the code against the customer's current cart without using it up. */
    @Transactional(readOnly = true)
    public CouponQuoteResponse quote(Long userId, String code) {
        BigDecimal subtotal = cartSubtotal(userId);
        Coupon coupon = couponRepository.findByCode(normalize(code)).orElseThrow(CouponService::invalid);
        Applied applied = evaluate(coupon, userId, subtotal, Instant.now());
        return new CouponQuoteResponse(coupon.getCode(), coupon.getType(), coupon.getDescription(), subtotal,
                applied.discount(), subtotal.subtract(applied.discount()));
    }

    /** Checkout path: locks the coupon row so concurrent orders cannot exceed the usage limit. */
    @Transactional
    public Applied lockAndEvaluate(Long userId, String code, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCodeForUpdate(normalize(code)).orElseThrow(CouponService::invalid);
        return evaluate(coupon, userId, subtotal, Instant.now());
    }

    @Transactional
    public void recordRedemption(Applied applied, Long userId, Long orderId) {
        Coupon coupon = applied.coupon();
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
        redemptionRepository.save(new CouponRedemption(coupon.getId(), userId, orderId, applied.discount()));
    }

    /** A cancelled order gives its coupon use back. */
    @Transactional
    public void release(Long orderId) {
        redemptionRepository.findByOrderId(orderId).ifPresent(redemption -> {
            couponRepository.findByIdForUpdate(redemption.getCouponId()).ifPresent(coupon -> {
                coupon.setUsedCount(Math.max(0, coupon.getUsedCount() - 1));
                couponRepository.save(coupon);
            });
            redemptionRepository.delete(redemption);
        });
    }

    Applied evaluate(Coupon coupon, Long userId, BigDecimal subtotal, Instant now) {
        if (!coupon.isActive()
                || (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt()))
                || (coupon.getEndsAt() != null && !now.isBefore(coupon.getEndsAt()))) {
            throw invalid();
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "This coupon has reached its usage limit");
        }
        if (coupon.isOncePerCustomer() && redemptionRepository.existsByCouponIdAndUserId(coupon.getId(), userId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "You have already used this coupon");
        }
        if (subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Add items worth at least ₹" + coupon.getMinOrderValue().stripTrailingZeros().toPlainString()
                            + " to use this coupon");
        }
        BigDecimal discount = switch (coupon.getType()) {
            case PERCENT -> {
                BigDecimal raw = subtotal.multiply(coupon.getValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
                yield coupon.getMaxDiscount() != null ? raw.min(coupon.getMaxDiscount()) : raw;
            }
            case FLAT -> coupon.getValue();
            // Shipping is free for every order for now, so there is nothing to take off yet.
            case FREE_SHIPPING -> BigDecimal.ZERO;
        };
        discount = discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
        return new Applied(coupon, discount);
    }

    private BigDecimal cartSubtotal(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        if (items.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }
        Map<Long, ProductVariant> variants = variantRepository
                .findAllById(items.stream().map(CartItem::getVariantId).toList()).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        Map<Long, Product> products = productRepository
                .findAllById(variants.values().stream().map(ProductVariant::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, SalePricingService.PriceInfo> prices = pricing.priceInfo(products.values());
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            ProductVariant variant = variants.get(item.getVariantId());
            Product product = variant == null ? null : products.get(variant.getProductId());
            if (product == null) {
                continue;
            }
            subtotal = subtotal.add(SalePricingService.unitPrice(product, prices.get(product.getId()))
                    .multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return subtotal;
    }

    private static BusinessException invalid() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "This coupon code is not valid");
    }
}
