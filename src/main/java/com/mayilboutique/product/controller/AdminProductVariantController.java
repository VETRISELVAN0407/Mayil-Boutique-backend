package com.mayilboutique.product.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.product.dto.request.CreateProductVariantRequest;
import com.mayilboutique.product.dto.request.UpdateProductVariantRequest;
import com.mayilboutique.product.dto.request.UpdateProductVariantStockRequest;
import com.mayilboutique.product.dto.response.ProductVariantResponse;
import com.mayilboutique.product.service.ProductVariantService;

@RestController
@RequestMapping("/api/admin/products/{productId}/variants")
@RequiredArgsConstructor
public class AdminProductVariantController {

    private final ProductVariantService productVariantService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductVariantResponse>> getVariants(@PathVariable Long productId) {
        List<ProductVariantResponse> variants = productVariantService.getVariants(productId);
        return ResponseEntity.ok(variants);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> createVariant(@PathVariable Long productId,
                                                                  @Valid @RequestBody CreateProductVariantRequest request) {
        ProductVariantResponse variant = productVariantService.createVariant(productId, request);
        return ResponseEntity.status(201).body(variant);
    }

    @PutMapping("/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> updateVariant(@PathVariable Long productId, @PathVariable Long variantId,
                                                                 @Valid @RequestBody UpdateProductVariantRequest request) {
        ProductVariantResponse variant = productVariantService.updateVariant(productId, variantId, request);
        return ResponseEntity.ok(variant);
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long productId, @PathVariable Long variantId) {
        productVariantService.deleteVariant(productId, variantId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{variantId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> adjustStock(@PathVariable Long productId, @PathVariable Long variantId,
                                                              @Valid @RequestBody UpdateProductVariantStockRequest request) {
        ProductVariantResponse variant = productVariantService.adjustStock(productId, variantId, request);
        return ResponseEntity.ok(variant);
    }
}
