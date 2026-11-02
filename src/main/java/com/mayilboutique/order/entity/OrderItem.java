package com.mayilboutique.order.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(length = 20)
    private String size;

    @Column(length = 40)
    private String colour;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    /** The product's cover image when the order was placed; kept so old orders still show what was bought. */
    @Column(name = "image_filename", length = 255)
    private String imageFilename;

    public OrderItem() {
    }

    public OrderItem(Long orderId, Long variantId, String productName, String sku, String size, String colour,
                     BigDecimal unitPrice, Integer quantity) {
        this(orderId, variantId, productName, sku, size, colour, unitPrice, quantity, null);
    }

    public OrderItem(Long orderId, Long variantId, String productName, String sku, String size, String colour,
                     BigDecimal unitPrice, Integer quantity, String imageFilename) {
        this.imageFilename = imageFilename;
        this.orderId = orderId;
        this.variantId = variantId;
        this.productName = productName;
        this.sku = sku;
        this.size = size;
        this.colour = colour;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getVariantId() { return variantId; }
    public String getProductName() { return productName; }
    public String getSku() { return sku; }
    public String getSize() { return size; }
    public String getColour() { return colour; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public String getImageFilename() { return imageFilename; }
}
