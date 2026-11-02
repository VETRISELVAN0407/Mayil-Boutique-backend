package com.mayilboutique.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.eq;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductVariant;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.repository.ProductVariantRepository;
import com.mayilboutique.stock.dto.AddStockRequest;
import com.mayilboutique.stock.dto.StockUpdateResponse;
import com.mayilboutique.stock.entity.StockUpdate;
import com.mayilboutique.stock.repository.StockUpdateRepository;

class StockUpdateServiceTest {

    private StockUpdateRepository stockUpdateRepository;
    private ProductVariantRepository variantRepository;
    private ProductRepository productRepository;
    private StockUpdateService service;
    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        stockUpdateRepository = mock(StockUpdateRepository.class);
        variantRepository = mock(ProductVariantRepository.class);
        productRepository = mock(ProductRepository.class);
        service = new StockUpdateService(stockUpdateRepository, variantRepository, productRepository);

        product = mock(Product.class);
        when(product.getId()).thenReturn(3L);
        when(product.getName()).thenReturn("Kurta");
        variant = mock(ProductVariant.class);
        when(variant.getId()).thenReturn(4L);
        when(variant.getProductId()).thenReturn(3L);
        when(variant.getStockQuantity()).thenReturn(15);
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));
        when(variantRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(variant));
        when(stockUpdateRepository.save(any(StockUpdate.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static AddStockRequest request(long productId, long variantId, String qty) {
        return new AddStockRequest(productId, variantId, new BigDecimal(qty));
    }

    @Test
    void addsToExistingStockAndRecordsHistory() {
        StockUpdateResponse response = service.addStock(request(3, 4, "10"));

        verify(variant).setStockQuantity(25);
        verify(variantRepository).save(variant);
        ArgumentCaptor<StockUpdate> saved = ArgumentCaptor.forClass(StockUpdate.class);
        verify(stockUpdateRepository).save(saved.capture());
        assertEquals(10, saved.getValue().getStockAdded());
        assertEquals(25, saved.getValue().getTotalStock());
        assertEquals(3L, saved.getValue().getProductId());
        assertEquals(4L, saved.getValue().getVariantId());
        assertEquals(25, response.totalStock());
        assertEquals(10, response.stockAdded());
    }

    @Test
    void readsTheVariantWithARowLock() {
        service.addStock(request(3, 4, "1"));

        verify(variantRepository).findByIdForUpdate(4L);
        verify(variantRepository, never()).findById(any());
    }

    @Test
    void unknownProductIsNotFoundAndNothingIsSaved() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.addStock(request(99, 4, "5")));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        verify(stockUpdateRepository, never()).save(any());
    }

    @Test
    void unknownVariantIsNotFound() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.addStock(request(3, 99, "5")));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void variantOfAnotherProductIsRejectedAndStockIsUntouched() {
        when(variant.getProductId()).thenReturn(8L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.addStock(request(3, 4, "5")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verify(variant, never()).setStockQuantity(any());
        verify(stockUpdateRepository, never()).save(any());
    }

    @Test
    void lowStockClampsThresholdAndPassesPage() {
        when(variantRepository.findLowStock(eq(1000), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(variantRepository.findLowStock(eq(0), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.lowStock(5000, 2, 500);
        service.lowStock(-3, -1, 0);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(variantRepository).findLowStock(eq(1000), pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
        verify(variantRepository).findLowStock(eq(0), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(1, pageable.getValue().getPageSize());
    }

    @Test
    void totalThatWouldOverflowIsRejected() {
        when(variant.getStockQuantity()).thenReturn(Integer.MAX_VALUE - 2);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.addStock(request(3, 4, "5")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verify(variant, never()).setStockQuantity(any());
        verify(stockUpdateRepository, never()).save(any());
    }
}
