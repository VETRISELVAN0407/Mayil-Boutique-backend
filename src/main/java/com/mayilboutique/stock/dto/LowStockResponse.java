package com.mayilboutique.stock.dto;

public record LowStockResponse(
        Long variantId,
        Long productId,
        String productName,
        String sku,
        String size,
        String sizeNumber,
        String colour,
        Integer stockQuantity) {
}
