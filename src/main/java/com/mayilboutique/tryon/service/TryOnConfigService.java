package com.mayilboutique.tryon.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.dto.response.ProductResponse;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductImage;
import com.mayilboutique.product.repository.ProductImageRepository;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.service.ProductResponseMapper;
import com.mayilboutique.tryon.dto.TryOnConfigResponse;
import com.mayilboutique.tryon.dto.TryOnProductResponse;
import com.mayilboutique.tryon.dto.UpdateTryOnConfigRequest;
import com.mayilboutique.tryon.entity.ProductTryOnConfig;
import com.mayilboutique.tryon.provider.TryOnCapabilities;
import com.mayilboutique.tryon.repository.ProductTryOnConfigRepository;

/** Admin settings for Virtual Try-On per product, and the storefront list of products that are eligible. */
@Service
public class TryOnConfigService {

    public static final int MAX_REFERENCE_IMAGES = 3;

    private final ProductTryOnConfigRepository configs;
    private final ProductRepository products;
    private final ProductImageRepository images;
    private final ProductResponseMapper mapper;
    private final TryOnCapabilities capabilities;

    public TryOnConfigService(ProductTryOnConfigRepository configs, ProductRepository products,
                              ProductImageRepository images, ProductResponseMapper mapper,
                              TryOnCapabilities capabilities) {
        this.configs = configs;
        this.products = products;
        this.images = images;
        this.mapper = mapper;
        this.capabilities = capabilities;
    }

    @Transactional(readOnly = true)
    public TryOnConfigResponse get(Long productId) {
        requireProduct(productId);
        return configs.findById(productId).map(TryOnConfigService::toResponse)
                .orElseGet(() -> new TryOnConfigResponse(productId, false, null, false, null, List.of()));
    }

    @Transactional
    public TryOnConfigResponse update(Long productId, UpdateTryOnConfigRequest request) {
        requireProduct(productId);
        boolean enabled = Boolean.TRUE.equals(request.getEnabled());
        boolean aiReady = Boolean.TRUE.equals(request.getAiReady());

        if ((enabled || aiReady) && request.getGarmentType() == null) {
            throw bad("Select a garment category for Virtual Try-On");
        }
        if ((enabled || aiReady) && !capabilities.isSupported(request.getGarmentType())) {
            throw bad("Virtual Try-On does not support this garment category yet");
        }
        if (aiReady && request.getPrimaryImageId() == null) {
            throw bad("Select the AI-ready garment image before marking the product ready");
        }

        Set<Long> references = new LinkedHashSet<>(
                request.getReferenceImageIds() == null ? List.of() : request.getReferenceImageIds());
        if (request.getPrimaryImageId() != null) {
            references.remove(request.getPrimaryImageId());
        }
        if (references.size() > MAX_REFERENCE_IMAGES) {
            throw bad("At most " + MAX_REFERENCE_IMAGES + " additional reference images are allowed");
        }

        // Every chosen image must belong to this product's own gallery.
        Set<Long> chosen = new LinkedHashSet<>(references);
        if (request.getPrimaryImageId() != null) {
            chosen.add(request.getPrimaryImageId());
        }
        Set<Long> gallery = images.findByProductIdOrderBySortOrderAscIdAsc(productId).stream()
                .map(ProductImage::getId).collect(Collectors.toSet());
        if (!gallery.containsAll(chosen)) {
            throw bad("Garment images must come from this product's own images");
        }

        ProductTryOnConfig config = configs.findById(productId).orElseGet(() -> new ProductTryOnConfig(productId));
        config.setEnabled(enabled);
        config.setGarmentType(request.getGarmentType());
        config.setAiReady(aiReady);
        config.setPrimaryImageId(request.getPrimaryImageId());
        config.getReferenceImageIds().clear();
        config.getReferenceImageIds().addAll(references);
        return toResponse(configs.save(config));
    }

    /** Published, enabled and ready products in shop order, optionally limited to one category. */
    @Transactional(readOnly = true)
    public List<TryOnProductResponse> listEligible(Long categoryId) {
        Map<Long, ProductTryOnConfig> byProduct = configs.findByEnabledTrueAndAiReadyTrueAndPrimaryImageIdIsNotNull()
                .stream().filter(ProductTryOnConfig::isEligible)
                .filter(c -> capabilities.isSupported(c.getGarmentType()))
                .collect(Collectors.toMap(ProductTryOnConfig::getProductId, Function.identity()));
        if (byProduct.isEmpty()) {
            return List.of();
        }
        List<Product> eligible = products.findAllById(byProduct.keySet()).stream()
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED)
                .filter(p -> categoryId == null || categoryId.equals(p.getCategoryId()))
                .sorted(Comparator.comparing(Product::getCreatedAt))
                .toList();
        List<TryOnProductResponse> result = new ArrayList<>();
        for (ProductResponse r : mapper.toPublicResponses(eligible)) {
            ProductTryOnConfig c = byProduct.get(r.getId());
            result.add(new TryOnProductResponse(r, c.getGarmentType(), c.getPrimaryImageId()));
        }
        return result;
    }

    /** 404 unless the product is published and eligible, so a hidden product reveals nothing. */
    @Transactional(readOnly = true)
    public TryOnProductResponse getEligible(Long productId) {
        ProductTryOnConfig config = configs.findById(productId).filter(ProductTryOnConfig::isEligible)
                .filter(c -> capabilities.isSupported(c.getGarmentType()))
                .orElseThrow(TryOnConfigService::notAvailable);
        Product product = products.findById(productId).filter(p -> p.getStatus() == ProductStatus.PUBLISHED)
                .orElseThrow(TryOnConfigService::notAvailable);
        return new TryOnProductResponse(mapper.toPublicResponse(product), config.getGarmentType(),
                config.getPrimaryImageId());
    }

    /** Stored filenames of the AI-ready garment image first, then the extra references. */
    @Transactional(readOnly = true)
    public List<String> garmentImageFilenames(Long productId) {
        ProductTryOnConfig config = configs.findById(productId).filter(ProductTryOnConfig::isEligible)
                .orElseThrow(TryOnConfigService::notAvailable);
        List<Long> ids = new ArrayList<>();
        ids.add(config.getPrimaryImageId());
        ids.addAll(config.getReferenceImageIds());
        Map<Long, String> names = images.findAllById(ids).stream()
                .collect(Collectors.toMap(ProductImage::getId, ProductImage::getFilename));
        if (!names.containsKey(config.getPrimaryImageId())) {
            throw notAvailable();
        }
        return ids.stream().map(names::get).filter(java.util.Objects::nonNull).toList();
    }

    private void requireProduct(Long productId) {
        if (!products.existsById(productId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Product not found");
        }
    }

    private static TryOnConfigResponse toResponse(ProductTryOnConfig c) {
        return new TryOnConfigResponse(c.getProductId(), c.isEnabled(), c.getGarmentType(), c.isAiReady(),
                c.getPrimaryImageId(), new ArrayList<>(c.getReferenceImageIds()));
    }

    private static BusinessException bad(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    private static BusinessException notAvailable() {
        return new BusinessException(HttpStatus.NOT_FOUND, "Virtual Try-On is not available for this product");
    }
}
