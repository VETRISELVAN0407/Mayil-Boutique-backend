package com.mayilboutique.product.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.mayilboutique.product.dto.response.ProductImageResponse;
import com.mayilboutique.product.dto.response.ProductResponse;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductImage;
import com.mayilboutique.product.entity.ProductVariant;
import com.mayilboutique.product.repository.ProductImageRepository;
import com.mayilboutique.product.repository.ProductVariantRepository;
import com.mayilboutique.sale.service.SalePricingService;

/**
 * Builds product responses with their images; lists load every product's images in one query.
 * {@code stockQuantity} is the total stock of the product's variants (the product-level column is not maintained).
 */
@Component
public class ProductResponseMapper {

    private final ProductImageRepository imageRepository;
    private final ProductVariantRepository variantRepository;
    private final SalePricingService pricing;

    public ProductResponseMapper(ProductImageRepository imageRepository, ProductVariantRepository variantRepository,
                                 SalePricingService pricing) {
        this.imageRepository = imageRepository;
        this.variantRepository = variantRepository;
        this.pricing = pricing;
    }

    /** Storefront view: {@code salePrice} is what the customer pays now (product sale price or running campaign). */
    public ProductResponse toPublicResponse(Product product) {
        ProductResponse response = toResponse(product);
        applyPricing(response, pricing.priceInfo(product));
        applyOptions(List.of(response));
        return response;
    }

    public List<ProductResponse> toPublicResponses(List<Product> products) {
        List<ProductResponse> responses = toResponses(products);
        Map<Long, SalePricingService.PriceInfo> info = pricing.priceInfo(products);
        responses.forEach(r -> applyPricing(r, info.get(r.getId())));
        applyOptions(responses);
        return responses;
    }

    /** Fills {@code sizes} and {@code colours} from the in-stock variants (one query for the whole list). */
    private void applyOptions(List<ProductResponse> responses) {
        if (responses.isEmpty()) {
            return;
        }
        List<Long> ids = responses.stream().map(ProductResponse::getId).toList();
        Map<Long, List<ProductVariant>> byProduct = variantRepository
                .findByProductIdInAndStockQuantityGreaterThan(ids, 0).stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));
        for (ProductResponse response : responses) {
            List<ProductVariant> variants = byProduct.getOrDefault(response.getId(), List.of());
            response.setSizes(variants.stream()
                    .map(v -> v.getSize() != null && !v.getSize().isBlank() ? v.getSize() : v.getSizeNumber())
                    .filter(s -> s != null && !s.isBlank()).map(String::trim).distinct().toList());
            response.setColours(variants.stream().map(ProductVariant::getColour)
                    .filter(c -> c != null && !c.isBlank()).map(String::trim).distinct().toList());
        }
    }

    private static void applyPricing(ProductResponse response, SalePricingService.PriceInfo info) {
        if (info != null && info.salePrice() != null) {
            response.setSalePrice(info.salePrice());
            response.setSaleName(info.saleName());
            response.setSaleEndsAt(info.saleEndsAt());
        }
    }

    public ProductResponse toResponse(Product product) {
        return build(product, imageRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()),
                variantStock(List.of(product.getId())));
    }

    public List<ProductResponse> toResponses(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        List<Long> ids = products.stream().map(Product::getId).toList();
        Map<Long, List<ProductImage>> images = imageRepository.findByProductIdInOrderBySortOrderAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));
        Map<Long, Integer> stock = variantStock(ids);
        return products.stream().map(p -> build(p, images.getOrDefault(p.getId(), List.of()), stock)).toList();
    }

    private Map<Long, Integer> variantStock(List<Long> productIds) {
        return variantRepository.sumStockByProductIds(productIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> Math.toIntExact(((Number) row[1]).longValue())));
    }

    private ProductResponse build(Product product, List<ProductImage> images, Map<Long, Integer> stock) {
        ProductResponse response = new ProductResponse(product.getId(), product.getName(), product.getSlug(),
                product.getDescription(), product.getCategoryId(), product.getPrice(), product.getSalePrice(),
                product.getSku(), product.getStatus(), stock.getOrDefault(product.getId(), 0),
                images.isEmpty() ? null : images.get(0).getFilename(), product.getCreatedAt(), product.getUpdatedAt());
        response.setImages(images.stream().map(i -> new ProductImageResponse(i.getId(), i.getFilename())).toList());
        return response;
    }
}
