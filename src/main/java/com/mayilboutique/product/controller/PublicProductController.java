package com.mayilboutique.product.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import com.mayilboutique.product.dto.response.ProductResponse;
import com.mayilboutique.product.dto.response.ProductVariantResponse;
import com.mayilboutique.product.service.ProductService;
import com.mayilboutique.product.service.ProductVariantService;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;

    @GetMapping("/public")
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(productService.getPublishedProducts(categoryId, search));
    }

    @GetMapping("/public/{slug}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getPublishedProductBySlug(slug));
    }

    @GetMapping("/public/{slug}/variants")
    public ResponseEntity<List<ProductVariantResponse>> getProductVariants(@PathVariable String slug) {
        ProductResponse product = productService.getPublishedProductBySlug(slug);
        return ResponseEntity.ok(productVariantService.getVariants(product.getId()));
    }
}
