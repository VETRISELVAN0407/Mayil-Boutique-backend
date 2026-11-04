package com.mayilboutique.product.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.mayilboutique.core.enums.ProductStatus;

public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long categoryId;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String sku;
    private ProductStatus status;
    private Integer stockQuantity;
    /** The cover image (first of {@link #images}), or null. Kept for callers that only need one picture. */
    private String imageFilename;
    /** All images, cover first. */
    private List<ProductImageResponse> images = List.of();
    private Instant createdAt;
    private Instant updatedAt;
    /** Running sale campaign that sets {@link #salePrice} (storefront responses only), else null. */
    private String saleName;
    private Instant saleEndsAt;
    /** Distinct sizes / colours of the in-stock variants (storefront responses only; empty otherwise). */
    private List<String> sizes = List.of();
    private List<String> colours = List.of();

    public ProductResponse() {
    }

    public ProductResponse(Long id, String name, String slug, String description, Long categoryId, BigDecimal price,
                           BigDecimal salePrice, String sku, ProductStatus status, Integer stockQuantity, Instant createdAt,
                           Instant updatedAt) {
        this(id, name, slug, description, categoryId, price, salePrice, sku, status, stockQuantity, null, createdAt,
                updatedAt);
    }

    public ProductResponse(Long id, String name, String slug, String description, Long categoryId, BigDecimal price,
                           BigDecimal salePrice, String sku, ProductStatus status, Integer stockQuantity,
                           String imageFilename, Instant createdAt, Instant updatedAt) {
        this.imageFilename = imageFilename;
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.categoryId = categoryId;
        this.price = price;
        this.salePrice = salePrice;
        this.sku = sku;
        this.status = status;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public void setImageFilename(String imageFilename) {
        this.imageFilename = imageFilename;
    }

    public List<ProductImageResponse> getImages() {
        return images;
    }

    public void setImages(List<ProductImageResponse> images) {
        this.images = images;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSaleName() {
        return saleName;
    }

    public void setSaleName(String saleName) {
        this.saleName = saleName;
    }

    public List<String> getSizes() {
        return sizes;
    }

    public void setSizes(List<String> sizes) {
        this.sizes = sizes;
    }

    public List<String> getColours() {
        return colours;
    }

    public void setColours(List<String> colours) {
        this.colours = colours;
    }

    public Instant getSaleEndsAt() {
        return saleEndsAt;
    }

    public void setSaleEndsAt(Instant saleEndsAt) {
        this.saleEndsAt = saleEndsAt;
    }
}
