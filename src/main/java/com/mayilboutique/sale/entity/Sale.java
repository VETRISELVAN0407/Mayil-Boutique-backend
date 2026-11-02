package com.mayilboutique.sale.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.mayilboutique.product.entity.Product;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/** A scheduled percentage-off campaign for all products, one category and/or chosen products. */
@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "percent_off", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentOff;

    @Column(name = "all_products", nullable = false)
    private boolean allProducts;

    @Column(name = "category_id")
    private Long categoryId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sale_products", joinColumns = @JoinColumn(name = "sale_id"))
    @Column(name = "product_id")
    private Set<Long> productIds = new HashSet<>();

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Sale() {
    }

    public boolean covers(Product product) {
        return allProducts
                || (categoryId != null && categoryId.equals(product.getCategoryId()))
                || productIds.contains(product.getId());
    }

    public boolean isRunningAt(Instant now) {
        return active && !now.isBefore(startsAt) && now.isBefore(endsAt);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPercentOff() { return percentOff; }
    public void setPercentOff(BigDecimal percentOff) { this.percentOff = percentOff; }
    public boolean isAllProducts() { return allProducts; }
    public void setAllProducts(boolean allProducts) { this.allProducts = allProducts; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Set<Long> getProductIds() { return productIds; }
    public void setProductIds(Set<Long> productIds) {
        this.productIds.clear();
        if (productIds != null) {
            this.productIds.addAll(productIds);
        }
    }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
