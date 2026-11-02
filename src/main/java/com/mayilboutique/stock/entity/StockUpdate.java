package com.mayilboutique.stock.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One row per "Add Stock" action: how much was added and the variant's total afterwards. */
@Entity
@Table(name = "stock_updates")
public class StockUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "variant_id", nullable = false, updatable = false)
    private Long variantId;

    @Column(name = "stock_added", nullable = false, updatable = false)
    private Integer stockAdded;

    @Column(name = "total_stock", nullable = false, updatable = false)
    private Integer totalStock;

    @CreationTimestamp
    @Column(name = "updated_on", nullable = false, updatable = false)
    private Instant updatedOn;

    protected StockUpdate() {
    }

    public StockUpdate(Long productId, Long variantId, Integer stockAdded, Integer totalStock) {
        this.productId = productId;
        this.variantId = variantId;
        this.stockAdded = stockAdded;
        this.totalStock = totalStock;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public Integer getStockAdded() {
        return stockAdded;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public Instant getUpdatedOn() {
        return updatedOn;
    }
}
