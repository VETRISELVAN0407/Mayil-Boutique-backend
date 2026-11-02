package com.mayilboutique.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReturnOrderRequest {

    @NotBlank(message = "Please tell us why you are returning the order")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    public ReturnOrderRequest() {
    }

    public ReturnOrderRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
