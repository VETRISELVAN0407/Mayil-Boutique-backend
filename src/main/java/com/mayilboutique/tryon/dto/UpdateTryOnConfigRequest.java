package com.mayilboutique.tryon.dto;

import java.util.List;

import com.mayilboutique.tryon.GarmentType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTryOnConfigRequest {

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;

    private GarmentType garmentType;

    @NotNull(message = "AI-ready flag is required")
    private Boolean aiReady;

    /** One of the product's own gallery images. */
    private Long primaryImageId;

    /** Optional extra gallery images; null or empty clears them. */
    private List<Long> referenceImageIds;
}
