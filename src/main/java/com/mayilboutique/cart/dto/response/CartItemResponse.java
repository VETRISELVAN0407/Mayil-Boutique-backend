package com.mayilboutique.cart.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public class CartItemResponse {

    private Long id;
    private Long variantId;
    private String variantSku;
    private String variantSize;
    private String variantColour;
    private Long productId;
    private String productName;
    private String productSlug;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Integer quantity;
    private Integer stockQuantity;
    private String imageFilename;
    private Instant updatedAt;

    public CartItemResponse() {
    }

    public CartItemResponse(Long id, Long variantId, String variantSku, String variantSize, String variantColour,
                            Long productId, String productName, String productSlug,
                            BigDecimal price, BigDecimal salePrice,
                            Integer quantity, Integer stockQuantity, String imageFilename, Instant updatedAt) {
        this.id = id;
        this.variantId = variantId;
        this.variantSku = variantSku;
        this.variantSize = variantSize;
        this.variantColour = variantColour;
        this.productId = productId;
        this.productName = productName;
        this.productSlug = productSlug;
        this.price = price;
        this.salePrice = salePrice;
        this.quantity = quantity;
        this.stockQuantity = stockQuantity;
        this.imageFilename = imageFilename;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getVariantId() { return variantId; }
    public String getVariantSku() { return variantSku; }
    public String getVariantSize() { return variantSize; }
    public String getVariantColour() { return variantColour; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductSlug() { return productSlug; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getSalePrice() { return salePrice; }
    public Integer getQuantity() { return quantity; }
    public Integer getStockQuantity() { return stockQuantity; }
    public String getImageFilename() { return imageFilename; }
    public Instant getUpdatedAt() { return updatedAt; }
}
