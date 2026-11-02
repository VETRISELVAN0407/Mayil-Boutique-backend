package com.mayilboutique.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.mayilboutique.cart.dto.request.MergeCartRequest;
import com.mayilboutique.cart.entity.CartItem;
import com.mayilboutique.cart.repository.CartItemRepository;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.entity.ProductVariant;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductImage;
import com.mayilboutique.product.repository.ProductImageRepository;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.repository.ProductVariantRepository;

class CartServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long VARIANT_ID = 3L;

    private CartItemRepository cartItemRepository;
    private ProductVariantRepository variantRepository;
    private ProductImageRepository imageRepository;
    private ProductRepository productRepository;
    private CartService service;

    @BeforeEach
    void setUp() {
        cartItemRepository = mock(CartItemRepository.class);
        variantRepository = mock(ProductVariantRepository.class);
        productRepository = mock(ProductRepository.class);
        imageRepository = mock(ProductImageRepository.class);
        service = new CartService(cartItemRepository, variantRepository, productRepository, imageRepository,
                new com.mayilboutique.sale.service.SalePricingService(org.mockito.Mockito.mock(com.mayilboutique.sale.repository.SaleRepository.class)));

        ProductVariant variant = new ProductVariant(10L, "SKU-M", "M", "Red", 5);
        when(variantRepository.findById(VARIANT_ID)).thenReturn(Optional.of(variant));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void addRejectsQuantityAboveStock() {
        when(cartItemRepository.findByUserIdAndVariantId(USER_ID, VARIANT_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.add(USER_ID, VARIANT_ID, 6));

        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void listCarriesTheCoverImageOfEachProduct() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(10L);
        ProductVariant variant = new ProductVariant(10L, "SKU-M", "M", "Red", 5);
        ReflectionTestUtils.setField(variant, "id", VARIANT_ID);
        when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of(new CartItem(USER_ID, VARIANT_ID, 1)));
        when(variantRepository.findAllById(any())).thenReturn(List.of(variant));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        // cover first, then a later image of the same product
        when(imageRepository.findByProductIdInOrderBySortOrderAscIdAsc(any())).thenReturn(List.of(
                new ProductImage(10L, "cover.jpg", 0), new ProductImage(10L, "second.jpg", 1)));

        assertEquals("cover.jpg", service.list(USER_ID).get(0).getImageFilename());
    }

    @Test
    void addReturnsTheCoverImage() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(10L);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserIdAndVariantId(USER_ID, VARIANT_ID)).thenReturn(Optional.empty());
        when(imageRepository.findFirstByProductIdOrderBySortOrderAscIdAsc(10L))
                .thenReturn(Optional.of(new ProductImage(10L, "cover.jpg", 0)));

        assertEquals("cover.jpg", service.add(USER_ID, VARIANT_ID, 1).getImageFilename());
    }

    @Test
    void addAllowsQuantityEqualToStock() {
        when(cartItemRepository.findByUserIdAndVariantId(USER_ID, VARIANT_ID)).thenReturn(Optional.empty());

        assertEquals(5, service.add(USER_ID, VARIANT_ID, 5).getQuantity());
    }

    @Test
    void updateRejectsQuantityAboveStock() {
        CartItem existing = new CartItem(USER_ID, VARIANT_ID, 1);
        when(cartItemRepository.findByUserIdAndVariantId(USER_ID, VARIANT_ID)).thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(USER_ID, VARIANT_ID, 9));

        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        assertEquals(1, existing.getQuantity());
    }

    @Test
    void mergeClampsGuestQuantityToStock() {
        when(cartItemRepository.findByUserIdAndVariantId(USER_ID, VARIANT_ID)).thenReturn(Optional.empty());
        MergeCartRequest.MergeCartItem guest = new MergeCartRequest.MergeCartItem();
        guest.setVariantId(VARIANT_ID);
        guest.setQuantity(50);
        MergeCartRequest request = new MergeCartRequest();
        request.setItems(List.of(guest));

        service.merge(USER_ID, request.getItems());

        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(saved.capture());
        assertEquals(5, saved.getValue().getQuantity());
    }
}
