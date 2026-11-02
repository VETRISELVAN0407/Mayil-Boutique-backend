package com.mayilboutique.product.dto.request;

import jakarta.validation.constraints.Min;
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
public class CreateProductVariantRequest {

    @NotBlank(message = "Variant SKU is required")
    @Size(max = 64, message = "Variant SKU must not exceed 64 characters")
    private String sku;

    @Size(max = 20, message = "Variant size must not exceed 20 characters")
    private String size;

    @Size(max = 10, message = "Size number must not exceed 10 characters")
    @Pattern(regexp = "^[0-9A-Za-z./-]*$", message = "Size number may only contain letters, digits, '.', '-' and '/'")
    private String sizeNumber;

    @Size(max = 40, message = "Variant colour must not exceed 40 characters")
    private String colour;

    @NotNull(message = "Initial stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;
}
