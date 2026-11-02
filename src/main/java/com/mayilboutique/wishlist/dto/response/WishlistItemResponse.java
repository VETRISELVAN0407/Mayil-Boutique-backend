package com.mayilboutique.wishlist.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public class WishlistItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Instant addedAt;

    public WishlistItemResponse() {
    }

    public WishlistItemResponse(Long id, Long productId, String productName, String productSlug,
                                BigDecimal price, BigDecimal salePrice, Instant addedAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productSlug = productSlug;
        this.price = price;
        this.salePrice = salePrice;
        this.addedAt = addedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductSlug() {
        return productSlug;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
