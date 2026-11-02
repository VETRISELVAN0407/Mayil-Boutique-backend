package com.mayilboutique.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mayilboutique.coupon.entity.CouponType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 32, message = "Coupon code must be 3 to 32 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Coupon code may only contain letters, digits, - and _")
    private String code;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @NotNull(message = "Coupon type is required")
    private CouponType type;

    @DecimalMin(value = "0", message = "Value cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Value can have at most 2 decimal places")
    private BigDecimal value;

    @DecimalMin(value = "0.01", message = "Maximum discount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Maximum discount can have at most 2 decimal places")
    private BigDecimal maxDiscount;

    @DecimalMin(value = "0", message = "Minimum order value cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Minimum order value can have at most 2 decimal places")
    private BigDecimal minOrderValue;

    private Instant startsAt;
    private Instant endsAt;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    private boolean oncePerCustomer;
    private Boolean active;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public CouponType getType() { return type; }
    public void setType(CouponType type) { this.type = type; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(BigDecimal maxDiscount) { this.maxDiscount = maxDiscount; }
    public BigDecimal getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(BigDecimal minOrderValue) { this.minOrderValue = minOrderValue; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public boolean isOncePerCustomer() { return oncePerCustomer; }
    public void setOncePerCustomer(boolean oncePerCustomer) { this.oncePerCustomer = oncePerCustomer; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
