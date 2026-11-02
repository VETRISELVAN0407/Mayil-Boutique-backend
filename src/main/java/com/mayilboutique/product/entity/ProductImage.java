package com.mayilboutique.product.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One stored image of a product. {@code filename} is the generated name only; the lowest {@code sortOrder} is the cover. */
@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 255, unique = true)
    private String filename;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ProductImage() {
    }

    public ProductImage(Long productId, String filename, int sortOrder) {
        this.productId = productId;
        this.filename = filename;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getFilename() { return filename; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
}
