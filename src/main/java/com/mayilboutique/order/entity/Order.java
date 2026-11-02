package com.mayilboutique.order.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.PaymentMethod;
import com.mayilboutique.core.enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String orderRef;

    @Column(nullable = false, length = 20)
    private String customerMobile;

    @Column(nullable = false, length = 255)
    private String customerName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Column(name = "ship_name", length = 255)
    private String shipName;

    @Column(name = "ship_mobile", length = 20)
    private String shipMobile;

    @Column(name = "ship_line1", length = 255)
    private String shipLine1;

    @Column(name = "ship_line2", length = 255)
    private String shipLine2;

    @Column(name = "ship_city", length = 100)
    private String shipCity;

    @Column(name = "ship_state", length = 100)
    private String shipState;

    @Column(name = "ship_pincode", length = 10)
    private String shipPincode;

    /** Staff-only; never part of a customer-facing response. */
    @Column(name = "internal_note", length = 1000)
    private String internalNote;

    @Column(name = "coupon_code", length = 32)
    private String couponCode;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "return_requested_at")
    private Instant returnRequestedAt;

    @Column(name = "return_reason", length = 500)
    private String returnReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Order() {
    }

    /** How long after delivery a customer may still ask for a return. */
    public static final java.time.Duration RETURN_WINDOW = java.time.Duration.ofDays(7);

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    public Instant getReturnRequestedAt() { return returnRequestedAt; }
    public void setReturnRequestedAt(Instant returnRequestedAt) { this.returnRequestedAt = returnRequestedAt; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }

    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getShipName() { return shipName; }
    public void setShipName(String shipName) { this.shipName = shipName; }
    public String getShipMobile() { return shipMobile; }
    public void setShipMobile(String shipMobile) { this.shipMobile = shipMobile; }
    public String getShipLine1() { return shipLine1; }
    public void setShipLine1(String shipLine1) { this.shipLine1 = shipLine1; }
    public String getShipLine2() { return shipLine2; }
    public void setShipLine2(String shipLine2) { this.shipLine2 = shipLine2; }
    public String getShipCity() { return shipCity; }
    public void setShipCity(String shipCity) { this.shipCity = shipCity; }
    public String getShipState() { return shipState; }
    public void setShipState(String shipState) { this.shipState = shipState; }
    public String getShipPincode() { return shipPincode; }
    public void setShipPincode(String shipPincode) { this.shipPincode = shipPincode; }

    public void setStatus(OrderStatus status) { this.status = status; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public Order(String orderRef, String customerMobile, String customerName, BigDecimal subtotal,
                 BigDecimal discountTotal, BigDecimal shippingTotal, BigDecimal taxTotal, BigDecimal total,
                 OrderStatus status, PaymentStatus paymentStatus) {
        this.orderRef = orderRef;
        this.customerMobile = customerMobile;
        this.customerName = customerName;
        this.subtotal = subtotal;
        this.discountTotal = discountTotal;
        this.shippingTotal = shippingTotal;
        this.taxTotal = taxTotal;
        this.total = total;
        this.status = status;
        this.paymentStatus = paymentStatus;
    }

    public Long getId() {
        return id;
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

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public BigDecimal getShippingTotal() {
        return shippingTotal;
    }

    public BigDecimal getTaxTotal() {
        return taxTotal;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
