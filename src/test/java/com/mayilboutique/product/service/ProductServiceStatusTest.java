package com.mayilboutique.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.mayilboutique.category.repository.CategoryRepository;
import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.repository.ProductImageRepository;
import com.mayilboutique.product.repository.ProductRepository;

class ProductServiceStatusTest {

    private ProductRepository products;
    private ProductService service;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        service = new ProductService(products, mock(CategoryRepository.class), mock(ProductImageRepository.class),
                mock(ProductImageService.class), mock(ProductImageFiles.class), mock(ProductResponseMapper.class));
        when(products.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Product productWith(ProductStatus status) {
        Product p = new Product("Kurta", "kurta", null, null, new BigDecimal("100"), null, "SKU-1", status, 0);
        when(products.findById(1L)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void archivedProductCanBeUnarchivedToDraft() {
        Product p = productWith(ProductStatus.ARCHIVED);

        service.updateStatus(1L, ProductStatus.DRAFT);

        assertEquals(ProductStatus.DRAFT, p.getStatus());
        verify(products).save(p);
    }

    @Test
    void archivedProductCannotGoStraightToPublished() {
        Product p = productWith(ProductStatus.ARCHIVED);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateStatus(1L, ProductStatus.PUBLISHED));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals(ProductStatus.ARCHIVED, p.getStatus());
        verify(products, never()).save(any());
    }

    @Test
    void draftCannotBeArchivedDirectly() {
        productWith(ProductStatus.DRAFT);

        assertThrows(BusinessException.class, () -> service.updateStatus(1L, ProductStatus.ARCHIVED));
    }
}
