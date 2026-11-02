package com.mayilboutique.sale.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SaleRequest {

    @NotBlank(message = "Sale name is required")
    @Size(max = 100, message = "Sale name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Percentage off is required")
    @DecimalMin(value = "1", message = "Percentage off must be at least 1")
    @DecimalMax(value = "90", message = "Percentage off cannot be more than 90")
    @Digits(integer = 2, fraction = 2, message = "Percentage off can have at most 2 decimal places")
    private BigDecimal percentOff;

    private boolean allProducts;
    private Long categoryId;
    private List<Long> productIds;

    @NotNull(message = "Start date is required")
    private Instant startsAt;

    @NotNull(message = "End date is required")
    private Instant endsAt;

    private Boolean active;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPercentOff() { return percentOff; }
    public void setPercentOff(BigDecimal percentOff) { this.percentOff = percentOff; }
    public boolean isAllProducts() { return allProducts; }
    public void setAllProducts(boolean allProducts) { this.allProducts = allProducts; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public List<Long> getProductIds() { return productIds; }
    public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
