package com.mayilboutique.tryon.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.JobStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One try-on generation. File columns hold generated filenames only and are nulled once the files are deleted. */
@Entity
@Table(name = "virtual_try_on_jobs")
public class VirtualTryOnJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "garment_type", nullable = false, length = 32)
    private GarmentType garmentType;

    @Column(length = 64)
    private String provider;

    @Column(name = "provider_job_id")
    private String providerJobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobStatus status;

    @Column(name = "original_photo_path")
    private String originalPhotoPath;

    @Column(name = "generated_image_path")
    private String generatedImagePath;

    /** A message safe to show the customer; never raw provider output. */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "consent_accepted_at", nullable = false)
    private Instant consentAcceptedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    public VirtualTryOnJob() {
    }

    public VirtualTryOnJob(Long customerId, Long productId, String productName, GarmentType garmentType,
                           String originalPhotoPath, Instant consentAcceptedAt, Instant expiresAt) {
        this.customerId = customerId;
        this.productId = productId;
        this.productName = productName;
        this.garmentType = garmentType;
        this.originalPhotoPath = originalPhotoPath;
        this.consentAcceptedAt = consentAcceptedAt;
        this.expiresAt = expiresAt;
        this.status = JobStatus.PENDING;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public GarmentType getGarmentType() { return garmentType; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderJobId() { return providerJobId; }
    public void setProviderJobId(String providerJobId) { this.providerJobId = providerJobId; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public String getOriginalPhotoPath() { return originalPhotoPath; }
    public void setOriginalPhotoPath(String originalPhotoPath) { this.originalPhotoPath = originalPhotoPath; }
    public String getGeneratedImagePath() { return generatedImagePath; }
    public void setGeneratedImagePath(String generatedImagePath) { this.generatedImagePath = generatedImagePath; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getConsentAcceptedAt() { return consentAcceptedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
