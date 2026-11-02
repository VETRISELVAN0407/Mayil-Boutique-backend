package com.mayilboutique.product.dto.request;

import com.mayilboutique.core.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductStatusRequest {

    @NotNull(message = "Status is required")
    private ProductStatus status;
}
