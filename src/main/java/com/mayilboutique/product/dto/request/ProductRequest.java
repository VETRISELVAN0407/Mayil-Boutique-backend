package com.mayilboutique.product.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Product slug is required")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Product slug must be lowercase alphanumeric with hyphens")
    private String slug;

    private String description;

    private Long categoryId;

    @NotNull(message = "Product price is required")
    @DecimalMin(value = "0.01", message = "Product price must be greater than zero")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "Product sale price must be greater than or equal to zero")
    private BigDecimal salePrice;

    @NotBlank(message = "Product SKU is required")
    @Size(max = 64, message = "Product SKU must not exceed 64 characters")
    private String sku;
}
