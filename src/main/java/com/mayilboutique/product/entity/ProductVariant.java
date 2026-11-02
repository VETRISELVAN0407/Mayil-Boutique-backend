package com.mayilboutique.product.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(length = 20)
    private String size;

    /** Numeric size shown next to the letter size, e.g. 32 for "M (32)". Optional. */
    @Column(name = "size_number", length = 10)
    private String sizeNumber;

    @Column(length = 40)
    private String colour;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Version
    @Column(name = "version")
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProductVariant() {
    }

    public ProductVariant(Long productId, String sku, String size, String colour, Integer stockQuantity) {
        this.productId = productId;
        this.sku = sku;
        this.size = size;
        this.colour = colour;
        this.stockQuantity = stockQuantity;
    }

    public ProductVariant(Long productId, String sku, String size, String sizeNumber, String colour, Integer stockQuantity) {
        this(productId, sku, size, colour, stockQuantity);
        this.sizeNumber = sizeNumber;
    }

    public Long getId() {
        return id;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
