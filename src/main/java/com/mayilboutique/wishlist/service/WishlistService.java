package com.mayilboutique.wishlist.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.sale.service.SalePricingService;
import com.mayilboutique.wishlist.dto.response.WishlistItemResponse;
import com.mayilboutique.wishlist.entity.WishlistItem;
import com.mayilboutique.wishlist.repository.WishlistRepository;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final SalePricingService pricing;

    public WishlistService(WishlistRepository wishlistRepository, ProductRepository productRepository,
                           SalePricingService pricing) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.pricing = pricing;
    }

    public List<WishlistItemResponse> list(Long userId) {
        List<WishlistItem> items = wishlistRepository.findByUserId(userId);
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = items.stream().map(WishlistItem::getProductId).toList();
        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return items.stream()
                .map(item -> toResponse(item, products.get(item.getProductId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public WishlistItemResponse add(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Product not found"));
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            WishlistItem existing = wishlistRepository.findByUserIdAndProductId(userId, productId).get();
            return toResponse(existing, product);
        }
        WishlistItem saved = wishlistRepository.save(new WishlistItem(userId, productId));
        return toResponse(saved, product);
    }

    @Transactional
    public void remove(Long userId, Long productId) {
        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Wishlist item not found");
        }
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public List<WishlistItemResponse> merge(Long userId, List<Long> guestProductIds) {
        if (guestProductIds == null || guestProductIds.isEmpty()) {
            return list(userId);
        }
        List<Long> validIds = productRepository.findAllById(guestProductIds).stream()
                .map(Product::getId)
                .toList();
        for (Long productId : validIds) {
            if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
                // guest wins — item already present, no quantity to overwrite; just ensure it exists
                continue;
            }
            wishlistRepository.save(new WishlistItem(userId, productId));
        }
        return list(userId);
    }

    private WishlistItemResponse toResponse(WishlistItem item, Product product) {
        if (product == null) {
            return new WishlistItemResponse(item.getId(), item.getProductId(),
                    null, null, null, null, item.getCreatedAt());
        }
        return new WishlistItemResponse(
                item.getId(),
                item.getProductId(),
                product.getName(),
                product.getSlug(),
                product.getPrice(),
                pricing.priceInfo(product).salePrice(),
                item.getCreatedAt());
    }
}
