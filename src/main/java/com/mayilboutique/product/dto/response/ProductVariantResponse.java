package com.mayilboutique.product.dto.response;

import java.time.Instant;

public class ProductVariantResponse {

    private Long id;
    private Long productId;
    private String sku;
    private String size;
    private String sizeNumber;
    private String colour;
    private Integer stockQuantity;
    private Instant createdAt;
    private Instant updatedAt;

    public ProductVariantResponse() {
    }

    public ProductVariantResponse(Long id, Long productId, String sku, String size, String colour, Integer stockQuantity,
                                  Instant createdAt, Instant updatedAt) {
        this(id, productId, sku, size, null, colour, stockQuantity, createdAt, updatedAt);
    }

    public ProductVariantResponse(Long id, Long productId, String sku, String size, String sizeNumber, String colour,
                                  Integer stockQuantity, Instant createdAt, Instant updatedAt) {
        this.sizeNumber = sizeNumber;
        this.id = id;
        this.productId = productId;
        this.sku = sku;
        this.size = size;
        this.colour = colour;
        this.stockQuantity = stockQuantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getSizeNumber() {
        return sizeNumber;
    }

    public void setSizeNumber(String sizeNumber) {
        this.sizeNumber = sizeNumber;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
