package com.mayilboutique.product.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mayilboutique.product.dto.request.ReorderProductImagesRequest;
import com.mayilboutique.product.dto.response.ProductResponse;
import com.mayilboutique.product.service.ProductImageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Every call returns the updated product, so the client can refresh from the response. */
@RestController
@RequestMapping("/api/admin/products/{productId}/images")
@RequiredArgsConstructor
public class AdminProductImageController {

    private final ProductImageService productImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> add(@PathVariable Long productId,
                                               @RequestPart("images") List<MultipartFile> images) {
        return ResponseEntity.ok(productImageService.add(productId, images));
    }

    @PutMapping("/order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> reorder(@PathVariable Long productId,
                                                   @Valid @RequestBody ReorderProductImagesRequest request) {
        return ResponseEntity.ok(productImageService.reorder(productId, request.getImageIds()));
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> delete(@PathVariable Long productId, @PathVariable Long imageId) {
        return ResponseEntity.ok(productImageService.delete(productId, imageId));
    }
}
