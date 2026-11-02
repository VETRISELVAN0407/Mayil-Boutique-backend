package com.mayilboutique.tryon.dto;

import com.mayilboutique.product.dto.response.ProductResponse;
import com.mayilboutique.tryon.GarmentType;

/** Storefront view of a try-on-eligible product. {@code primaryImageId} points into {@code product.images}. */
public record TryOnProductResponse(ProductResponse product, GarmentType garmentType, Long primaryImageId) {
}
