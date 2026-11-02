package com.mayilboutique.stock.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Quantity is a BigDecimal only so that 2.5 is rejected instead of silently truncated to 2. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AddStockRequest {

    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Variant is required")
    private Long variantId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "1", message = "Quantity must be at least 1")
    @DecimalMax(value = "100000", message = "Quantity cannot be more than 100000 at a time")
    @Digits(integer = 6, fraction = 0, message = "Quantity must be a whole number")
    private BigDecimal quantity;
}
