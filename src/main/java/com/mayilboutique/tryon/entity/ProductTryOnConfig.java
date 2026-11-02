package com.mayilboutique.tryon.entity;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.UpdateTimestamp;

import com.mayilboutique.tryon.GarmentType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_tryon_config")
public class ProductTryOnConfig {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "garment_type", length = 32)
    private GarmentType garmentType;

    @Column(name = "ai_ready", nullable = false)
    private boolean aiReady;

    @Column(name = "primary_image_id")
    private Long primaryImageId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_tryon_reference_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_id")
    private Set<Long> referenceImageIds = new LinkedHashSet<>();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProductTryOnConfig() {
    }

    public ProductTryOnConfig(Long productId) {
        this.productId = productId;
    }

    public Long getProductId() { return productId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public GarmentType getGarmentType() { return garmentType; }
    public void setGarmentType(GarmentType garmentType) { this.garmentType = garmentType; }
    public boolean isAiReady() { return aiReady; }
    public void setAiReady(boolean aiReady) { this.aiReady = aiReady; }
    public Long getPrimaryImageId() { return primaryImageId; }
    public void setPrimaryImageId(Long primaryImageId) { this.primaryImageId = primaryImageId; }
    public Set<Long> getReferenceImageIds() { return referenceImageIds; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** Usable by customers: switched on, marked ready, and still has its AI image. */
    public boolean isEligible() {
        return enabled && aiReady && garmentType != null && primaryImageId != null;
    }
}
