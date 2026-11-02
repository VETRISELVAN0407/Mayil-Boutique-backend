package com.mayilboutique.order.dto.request;

import jakarta.validation.constraints.NotNull;

public class PlaceOrderRequest {

    @NotNull
    private Long addressId;

    @jakarta.validation.constraints.Size(max = 32, message = "Coupon code is too long")
    private String couponCode;

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
