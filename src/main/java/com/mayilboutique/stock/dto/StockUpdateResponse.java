package com.mayilboutique.stock.dto;

import java.time.Instant;

public record StockUpdateResponse(
        Long id,
        Long productId,
        String productName,
        Long variantId,
        String sku,
        String size,
        String colour,
        Integer stockAdded,
        Integer totalStock,
        Instant updatedOn) {
}
