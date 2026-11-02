package com.mayilboutique.product.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReorderProductImagesRequest {

    /** Every image id of the product, in the wanted order; the first is the cover. */
    @NotEmpty(message = "Image ids are required")
    private List<Long> imageIds;
}
