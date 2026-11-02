package com.mayilboutique.order.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.PaymentMethod;
import com.mayilboutique.core.enums.PaymentStatus;
import com.mayilboutique.order.entity.Order;
import com.mayilboutique.order.entity.OrderItem;

public record OrderResponse(
        String orderRef,
        OrderStatus status,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal shippingTotal,
        BigDecimal taxTotal,
        BigDecimal total,
        ShippingAddress shippingAddress,
        List<Line> items,
        Instant createdAt,
        String couponCode,
        Instant deliveredAt,
        Instant returnEligibleUntil,
        Instant returnRequestedAt,
        String returnReason,
        @JsonInclude(JsonInclude.Include.NON_NULL) String internalNote) {

    public record ShippingAddress(String name, String mobile, String line1, String line2,
                                  String city, String state, String pincode) {
    }

    public record Line(Long variantId, String productName, String sku, String size, String colour,
                       BigDecimal unitPrice, Integer quantity, BigDecimal lineTotal, String imageFilename) {
    }

    /** Customer-facing view: the internal note is never included. */
    public static OrderResponse from(Order o, List<OrderItem> items) {
        return build(o, items, null);
    }

    /** Staff view: same as {@link #from} plus the internal note (omitted from JSON while empty). */
    public static OrderResponse forAdmin(Order o, List<OrderItem> items) {
        return build(o, items, o.getInternalNote());
    }

    private static OrderResponse build(Order o, List<OrderItem> items, String internalNote) {
        return new OrderResponse(
                o.getOrderRef(), o.getStatus(), o.getPaymentStatus(), o.getPaymentMethod(),
                o.getSubtotal(), o.getDiscountTotal(), o.getShippingTotal(), o.getTaxTotal(), o.getTotal(),
                new ShippingAddress(o.getShipName(), o.getShipMobile(), o.getShipLine1(), o.getShipLine2(),
                        o.getShipCity(), o.getShipState(), o.getShipPincode()),
                items.stream()
                        .map(i -> new Line(i.getVariantId(), i.getProductName(), i.getSku(), i.getSize(),
                                i.getColour(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal(),
                                i.getImageFilename()))
                        .toList(),
                o.getCreatedAt(),
                o.getCouponCode(),
                o.getDeliveredAt(),
                o.getStatus() == OrderStatus.DELIVERED && o.getDeliveredAt() != null
                        ? o.getDeliveredAt().plus(Order.RETURN_WINDOW) : null,
                o.getReturnRequestedAt(),
                o.getReturnReason(),
                internalNote);
    }
}
