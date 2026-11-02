package com.mayilboutique.product.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.dto.request.CreateProductVariantRequest;
import com.mayilboutique.product.dto.request.UpdateProductVariantRequest;
import com.mayilboutique.product.dto.request.UpdateProductVariantStockRequest;
import com.mayilboutique.product.dto.response.ProductVariantResponse;
import com.mayilboutique.product.entity.ProductVariant;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.repository.ProductVariantRepository;

@Service
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    public ProductVariantService(ProductVariantRepository productVariantRepository, ProductRepository productRepository) {
        this.productVariantRepository = productVariantRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariants(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return productVariantRepository.findAllByProductIdOrderByCreatedAtAsc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductVariantResponse createVariant(Long productId, CreateProductVariantRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Product not found");
        }
        if (productVariantRepository.existsBySku(request.getSku())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Product variant SKU already exists");
        }
        ProductVariant variant = new ProductVariant(productId, request.getSku(), request.getSize(),
                blankToNull(request.getSizeNumber()), request.getColour(), request.getStockQuantity());
        return toResponse(productVariantRepository.save(variant));
    }

    @Transactional
    public ProductVariantResponse updateVariant(Long productId, Long variantId, UpdateProductVariantRequest request) {
        ProductVariant variant = getVariantOrThrow(productId, variantId);
        if (productVariantRepository.existsBySkuAndIdNot(request.getSku(), variant.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Product variant SKU already exists");
        }
        variant.setSku(request.getSku());
        variant.setSize(request.getSize());
        variant.setSizeNumber(blankToNull(request.getSizeNumber()));
        variant.setColour(request.getColour());
        variant.setStockQuantity(request.getStockQuantity());
        return toResponse(productVariantRepository.save(variant));
    }

    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        ProductVariant variant = getVariantOrThrow(productId, variantId);
        productVariantRepository.delete(variant);
    }

    @Transactional
    public ProductVariantResponse adjustStock(Long productId, Long variantId, UpdateProductVariantStockRequest request) {
        ProductVariant variant = getVariantOrThrow(productId, variantId);
        if (request.getStockQuantity() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Stock quantity cannot be negative");
        }
        variant.setStockQuantity(request.getStockQuantity());
        return toResponse(productVariantRepository.save(variant));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ProductVariant getVariantOrThrow(Long productId, Long variantId) {
        return productVariantRepository.findById(variantId)
                .filter(variant -> variant.getProductId().equals(productId))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Product variant not found"));
    }

    private ProductVariantResponse toResponse(ProductVariant variant) {
        return new ProductVariantResponse(variant.getId(), variant.getProductId(), variant.getSku(), variant.getSize(),
                variant.getSizeNumber(), variant.getColour(), variant.getStockQuantity(), variant.getCreatedAt(), variant.getUpdatedAt());
    }
}
