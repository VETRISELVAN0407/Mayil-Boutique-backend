package com.mayilboutique.tryon.dto;

import java.util.List;

import com.mayilboutique.tryon.GarmentType;

public record TryOnConfigResponse(Long productId, boolean enabled, GarmentType garmentType, boolean aiReady,
                                  Long primaryImageId, List<Long> referenceImageIds) {
}
