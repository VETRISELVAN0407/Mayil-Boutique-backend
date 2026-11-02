package com.mayilboutique.sale.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.product.entity.Product;
import com.mayilboutique.sale.entity.Sale;
import com.mayilboutique.sale.repository.SaleRepository;

/**
 * The single place that decides what a customer pays per unit: the product's own sale price or the best running
 * sale campaign, whichever is lower. Cart, wishlist, storefront and checkout all go through here.
 */
@Service
public class SalePricingService {

    /** {@code salePrice} is the price to charge, or null when it is just the regular price. */
    public record PriceInfo(BigDecimal salePrice, String saleName, Instant saleEndsAt) {
        public static final PriceInfo NONE = new PriceInfo(null, null, null);
    }

    private final SaleRepository saleRepository;

    public SalePricingService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, PriceInfo> priceInfo(Collection<Product> products) {
        List<Sale> running = saleRepository.findRunning(Instant.now());
        Map<Long, PriceInfo> result = new HashMap<>();
        for (Product product : products) {
            result.put(product.getId(), priceOf(product, running));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PriceInfo priceInfo(Product product) {
        return priceOf(product, saleRepository.findRunning(Instant.now()));
    }

    /** What one unit costs now. */
    public static BigDecimal unitPrice(Product product, PriceInfo info) {
        return info != null && info.salePrice() != null ? info.salePrice() : product.getPrice();
    }

    static PriceInfo priceOf(Product product, List<Sale> running) {
        BigDecimal best = product.getSalePrice() != null ? product.getSalePrice() : null;
        Sale winner = null;
        for (Sale sale : running) {
            if (!sale.covers(product)) {
                continue;
            }
            BigDecimal discounted = product.getPrice()
                    .multiply(BigDecimal.valueOf(100).subtract(sale.getPercentOff()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (best == null || discounted.compareTo(best) < 0) {
                best = discounted;
                winner = sale;
            }
        }
        if (best == null) {
            return PriceInfo.NONE;
        }
        return new PriceInfo(best, winner == null ? null : winner.getName(), winner == null ? null : winner.getEndsAt());
    }
}
