package com.mayilboutique.sale.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.sale.entity.Sale;
import com.mayilboutique.sale.service.SalePricingService.PriceInfo;

class SalePricingServiceTest {

    private static Product product(Long id, Long categoryId, String price, String salePrice) {
        Product p = new Product();
        ReflectionTestUtils.setField(p, "id", id);
        p.setCategoryId(categoryId);
        p.setPrice(new BigDecimal(price));
        p.setSalePrice(salePrice == null ? null : new BigDecimal(salePrice));
        p.setStatus(ProductStatus.PUBLISHED);
        return p;
    }

    private static Sale sale(String name, String percent) {
        Sale s = new Sale();
        s.setName(name);
        s.setPercentOff(new BigDecimal(percent));
        s.setStartsAt(Instant.now().minusSeconds(60));
        s.setEndsAt(Instant.now().plusSeconds(3600));
        return s;
    }

    @Test
    void noSaleMeansRegularPrice() {
        PriceInfo info = SalePricingService.priceOf(product(1L, 1L, "1000", null), List.of());
        assertNull(info.salePrice());
        assertEquals(new BigDecimal("1000"), SalePricingService.unitPrice(product(1L, 1L, "1000", null), info));
    }

    @Test
    void categorySaleDiscountsProductsInThatCategoryOnly() {
        Sale s = sale("Diwali", "20");
        s.setCategoryId(5L);

        PriceInfo inCategory = SalePricingService.priceOf(product(1L, 5L, "1000", null), List.of(s));
        PriceInfo elsewhere = SalePricingService.priceOf(product(2L, 6L, "1000", null), List.of(s));

        assertEquals(new BigDecimal("800.00"), inCategory.salePrice());
        assertEquals("Diwali", inCategory.saleName());
        assertNull(elsewhere.salePrice());
    }

    @Test
    void chosenProductsAndAllProductsScopesWork() {
        Sale chosen = sale("Pick", "10");
        chosen.setProductIds(Set.of(1L));
        Sale all = sale("Everything", "5");
        all.setAllProducts(true);

        assertEquals(new BigDecimal("900.00"), SalePricingService.priceOf(product(1L, 1L, "1000", null), List.of(chosen)).salePrice());
        assertNull(SalePricingService.priceOf(product(2L, 1L, "1000", null), List.of(chosen)).salePrice());
        assertEquals(new BigDecimal("950.00"), SalePricingService.priceOf(product(2L, 1L, "1000", null), List.of(all)).salePrice());
    }

    @Test
    void theLowestPriceWinsBetweenCampaignsAndTheProductsOwnSalePrice() {
        Sale small = sale("Small", "10");
        small.setAllProducts(true);
        Sale big = sale("Big", "30");
        big.setAllProducts(true);

        assertEquals("Big", SalePricingService.priceOf(product(1L, 1L, "1000", null), List.of(small, big)).saleName());

        // The product's own sale price (600) beats a 10% campaign (900), and no campaign name is reported.
        PriceInfo ownWins = SalePricingService.priceOf(product(1L, 1L, "1000", "600"), List.of(small));
        assertEquals(new BigDecimal("600"), ownWins.salePrice());
        assertNull(ownWins.saleName());
    }

    @Test
    void saleRunsOnlyInsideItsWindowAndWhileActive() {
        Sale s = sale("Window", "10");
        assertEquals(true, s.isRunningAt(Instant.now()));
        assertEquals(false, s.isRunningAt(Instant.now().plusSeconds(7200)));
        s.setActive(false);
        assertEquals(false, s.isRunningAt(Instant.now()));
    }
}
