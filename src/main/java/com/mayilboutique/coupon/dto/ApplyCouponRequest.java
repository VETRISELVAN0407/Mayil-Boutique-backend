package com.mayilboutique.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ApplyCouponRequest {

    @NotBlank(message = "Enter a coupon code")
    @Size(max = 32, message = "Coupon code is too long")
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
