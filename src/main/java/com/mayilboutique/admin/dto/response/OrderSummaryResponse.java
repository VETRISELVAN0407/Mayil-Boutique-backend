package com.mayilboutique.admin.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.PaymentStatus;

public class OrderSummaryResponse {

    private String orderRef;
    private String customerMobile;
    private String customerName;
    private BigDecimal total;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private Instant createdAt;

    public OrderSummaryResponse() {
    }

    public OrderSummaryResponse(String orderRef, String customerMobile, String customerName, BigDecimal total,
                                OrderStatus status, PaymentStatus paymentStatus, Instant createdAt) {
        this.orderRef = orderRef;
        this.customerMobile = customerMobile;
        this.customerName = customerName;
        this.total = total;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    public String getOrderRef() {
        return orderRef;
    }

    public String getCustomerMobile() {
        return customerMobile;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setOrderRef(String orderRef) {
        this.orderRef = orderRef;
    }

    public void setCustomerMobile(String customerMobile) {
        this.customerMobile = customerMobile;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
