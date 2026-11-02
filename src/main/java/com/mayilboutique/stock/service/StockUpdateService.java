package com.mayilboutique.stock.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.core.dto.PageResponse;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductVariant;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.repository.ProductVariantRepository;
import com.mayilboutique.stock.dto.AddStockRequest;
import com.mayilboutique.stock.dto.LowStockResponse;
import com.mayilboutique.stock.dto.StockUpdateResponse;
import com.mayilboutique.stock.entity.StockUpdate;
import com.mayilboutique.stock.repository.StockUpdateRepository;

@Service
public class StockUpdateService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_THRESHOLD = 1000;

    private final StockUpdateRepository stockUpdateRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    public StockUpdateService(StockUpdateRepository stockUpdateRepository, ProductVariantRepository variantRepository,
                              ProductRepository productRepository) {
        this.stockUpdateRepository = stockUpdateRepository;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
    }

    /**
     * Adds to (never replaces) the variant's stock and records the history row in one transaction.
     * The variant row is locked for the duration, so two simultaneous additions are applied one
     * after the other and neither overwrites the other.
     */
    @Transactional
    public StockUpdateResponse addStock(AddStockRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Product not found"));
        ProductVariant variant = variantRepository.findByIdForUpdate(request.getVariantId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Product variant not found"));
        if (!variant.getProductId().equals(product.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "The variant does not belong to the selected product");
        }

        int added = request.getQuantity().intValueExact();
        long total = (long) variant.getStockQuantity() + added;
        if (total > Integer.MAX_VALUE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Total stock would be too large");
        }

        variant.setStockQuantity((int) total);
        variantRepository.save(variant);
        StockUpdate saved = stockUpdateRepository.save(new StockUpdate(product.getId(), variant.getId(), added, (int) total));

        return new StockUpdateResponse(saved.getId(), product.getId(), product.getName(), variant.getId(),
                variant.getSku(), variant.getSize(), variant.getColour(), added, (int) total, saved.getUpdatedOn());
    }

    /** Variants of published products with stock at or below {@code threshold} (0 = out of stock), lowest first. */
    @Transactional(readOnly = true)
    public PageResponse<LowStockResponse> lowStock(int threshold, int page, int size) {
        int limit = Math.min(Math.max(threshold, 0), MAX_THRESHOLD);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.of(variantRepository.findLowStock(limit, pageable), item -> item);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockUpdateResponse> history(Long variantId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.of(variantId == null
                ? stockUpdateRepository.findHistory(pageable)
                : stockUpdateRepository.findHistoryByVariant(variantId, pageable), item -> item);
    }
}
