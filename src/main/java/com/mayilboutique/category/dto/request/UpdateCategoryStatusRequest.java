package com.mayilboutique.category.dto.request;

import com.mayilboutique.core.enums.CategoryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryStatusRequest {

    @NotNull(message = "Status is required")
    private CategoryStatus status;
}
