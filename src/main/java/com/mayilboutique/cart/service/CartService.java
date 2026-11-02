package com.mayilboutique.cart.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.cart.dto.request.MergeCartRequest;
import com.mayilboutique.cart.dto.response.CartItemResponse;
import com.mayilboutique.cart.entity.CartItem;
import com.mayilboutique.cart.repository.CartItemRepository;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductImage;
import com.mayilboutique.product.entity.ProductVariant;
import com.mayilboutique.product.repository.ProductImageRepository;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.repository.ProductVariantRepository;
import com.mayilboutique.sale.service.SalePricingService;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final SalePricingService pricing;

    public CartService(CartItemRepository cartItemRepository,
                       ProductVariantRepository variantRepository,
                       ProductRepository productRepository,
                       ProductImageRepository imageRepository,
                       SalePricingService pricing) {
        this.imageRepository = imageRepository;
        this.pricing = pricing;
        this.cartItemRepository = cartItemRepository;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
    }

    public List<CartItemResponse> list(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        return buildResponses(items);
    }

    @Transactional
    public CartItemResponse add(Long userId, Long variantId, Integer quantity) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Variant not found"));
        if (variant.getStockQuantity() == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "Variant is out of stock");
        }
        requireStock(variant, quantity);
        CartItem item = cartItemRepository.findByUserIdAndVariantId(userId, variantId)
                .orElse(null);
        if (item != null) {
            item.setQuantity(quantity);
            item = cartItemRepository.save(item);
        } else {
            item = cartItemRepository.save(new CartItem(userId, variantId, quantity));
        }
        Product product = productRepository.findById(variant.getProductId()).orElse(null);
        return toResponse(item, variant, product, coverOf(product),
                product == null ? null : pricing.priceInfo(product).salePrice());
    }

    @Transactional
    public CartItemResponse update(Long userId, Long variantId, Integer quantity) {
        CartItem item = cartItemRepository.findByUserIdAndVariantId(userId, variantId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Cart item not found"));
        ProductVariant variant = variantRepository.findById(variantId).orElse(null);
        if (variant != null) {
            requireStock(variant, quantity);
        }
        item.setQuantity(quantity);
        item = cartItemRepository.save(item);
        Product product = variant != null ? productRepository.findById(variant.getProductId()).orElse(null) : null;
        return toResponse(item, variant, product, coverOf(product),
                product == null ? null : pricing.priceInfo(product).salePrice());
    }

    @Transactional
    public void remove(Long userId, Long variantId) {
        CartItem item = cartItemRepository.findByUserIdAndVariantId(userId, variantId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Cart item not found"));
        cartItemRepository.delete(item);
    }

    @Transactional
    public List<CartItemResponse> merge(Long userId, List<MergeCartRequest.MergeCartItem> guestItems) {
        if (guestItems == null || guestItems.isEmpty()) {
            return list(userId);
        }
        for (MergeCartRequest.MergeCartItem guestItem : guestItems) {
            ProductVariant variant = variantRepository.findById(guestItem.getVariantId()).orElse(null);
            if (variant == null || variant.getStockQuantity() == 0) {
                continue;
            }
            int quantity = Math.min(guestItem.getQuantity(), variant.getStockQuantity());
            cartItemRepository.findByUserIdAndVariantId(userId, guestItem.getVariantId())
                    .ifPresentOrElse(
                            existing -> {
                                // guest wins — overwrite qty
                                existing.setQuantity(quantity);
                                cartItemRepository.save(existing);
                            },
                            () -> cartItemRepository.save(new CartItem(userId, guestItem.getVariantId(), quantity))
                    );
        }
        return list(userId);
    }

    private void requireStock(ProductVariant variant, Integer quantity) {
        if (quantity > variant.getStockQuantity()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Only " + variant.getStockQuantity() + " in stock");
        }
    }

    private List<CartItemResponse> buildResponses(List<CartItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> variantIds = items.stream().map(CartItem::getVariantId).toList();
        Map<Long, ProductVariant> variants = variantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        List<Long> productIds = variants.values().stream().map(ProductVariant::getProductId).distinct().toList();
        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        // Images come back cover-first, so the first one seen for a product is its cover.
        Map<Long, String> covers = imageRepository.findByProductIdInOrderBySortOrderAscIdAsc(productIds).stream()
                .collect(Collectors.toMap(ProductImage::getProductId, ProductImage::getFilename, (first, later) -> first));
        Map<Long, SalePricingService.PriceInfo> prices = pricing.priceInfo(products.values());
        return items.stream()
                .map(item -> {
                    ProductVariant variant = variants.get(item.getVariantId());
                    Product product = variant != null ? products.get(variant.getProductId()) : null;
                    return toResponse(item, variant, product, product != null ? covers.get(product.getId()) : null,
                            product != null ? prices.get(product.getId()).salePrice() : null);
                })
                .collect(Collectors.toList());
    }

    private String coverOf(Product product) {
        return product == null ? null : imageRepository.findFirstByProductIdOrderBySortOrderAscIdAsc(product.getId())
                .map(ProductImage::getFilename).orElse(null);
    }

    private CartItemResponse toResponse(CartItem item, ProductVariant variant, Product product, String imageFilename,
                                        java.math.BigDecimal salePrice) {
        return new CartItemResponse(
                item.getId(),
                item.getVariantId(),
                variant != null ? variant.getSku() : null,
                variant != null ? variant.getSize() : null,
                variant != null ? variant.getColour() : null,
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                product != null ? product.getSlug() : null,
                product != null ? product.getPrice() : null,
                salePrice,
                item.getQuantity(),
                variant != null ? variant.getStockQuantity() : null,
                imageFilename,
                item.getUpdatedAt());
    }
}
