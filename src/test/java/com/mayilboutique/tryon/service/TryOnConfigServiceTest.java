package com.mayilboutique.tryon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.dto.response.ProductResponse;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductImage;
import com.mayilboutique.product.repository.ProductImageRepository;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.service.ProductResponseMapper;
import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.dto.TryOnConfigResponse;
import com.mayilboutique.tryon.dto.TryOnProductResponse;
import com.mayilboutique.tryon.dto.UpdateTryOnConfigRequest;
import com.mayilboutique.tryon.entity.ProductTryOnConfig;
import com.mayilboutique.tryon.provider.TryOnCapabilities;
import com.mayilboutique.tryon.repository.ProductTryOnConfigRepository;

class TryOnConfigServiceTest {

    private static final Long PRODUCT_ID = 1L;

    private ProductTryOnConfigRepository configs;
    private ProductRepository products;
    private ProductImageRepository images;
    private ProductResponseMapper mapper;
    private TryOnCapabilities capabilities;
    private TryOnConfigService service;

    @BeforeEach
    void setUp() {
        configs = mock(ProductTryOnConfigRepository.class);
        products = mock(ProductRepository.class);
        images = mock(ProductImageRepository.class);
        mapper = mock(ProductResponseMapper.class);
        capabilities = mock(TryOnCapabilities.class);
        when(capabilities.isSupported(any())).thenReturn(true);
        service = new TryOnConfigService(configs, products, images, mapper, capabilities);
        when(products.existsById(PRODUCT_ID)).thenReturn(true);
        when(images.findByProductIdOrderBySortOrderAscIdAsc(PRODUCT_ID))
                .thenReturn(List.of(image(10L), image(11L), image(12L), image(13L), image(14L)));
        when(configs.save(any(ProductTryOnConfig.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ProductImage image(long id) {
        ProductImage i = new ProductImage(PRODUCT_ID, "f" + id + ".jpg", (int) id);
        ReflectionTestUtils.setField(i, "id", id);
        return i;
    }

    private UpdateTryOnConfigRequest request(boolean enabled, GarmentType type, boolean ready, Long primary,
                                             List<Long> refs) {
        return new UpdateTryOnConfigRequest(enabled, type, ready, primary, refs);
    }

    private static void assertBadRequest(Runnable action) {
        BusinessException ex = assertThrows(BusinessException.class, action::run);
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void unknownProductIs404() {
        when(products.existsById(99L)).thenReturn(false);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void productWithoutConfigReadsAsDisabled() {
        when(configs.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        TryOnConfigResponse r = service.get(PRODUCT_ID);
        assertFalse(r.enabled());
        assertFalse(r.aiReady());
    }

    @Test
    void savesPrimaryAndReferenceImages() {
        when(configs.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        TryOnConfigResponse r = service.update(PRODUCT_ID,
                request(true, GarmentType.SAREE, true, 10L, List.of(11L, 12L)));
        assertTrue(r.enabled());
        assertEquals(GarmentType.SAREE, r.garmentType());
        assertEquals(10L, r.primaryImageId());
        assertEquals(List.of(11L, 12L), r.referenceImageIds());
    }

    @Test
    void enablingNeedsAGarmentType() {
        assertBadRequest(() -> service.update(PRODUCT_ID, request(true, null, false, null, null)));
        verify(configs, never()).save(any());
    }

    @Test
    void readyNeedsAPrimaryImage() {
        assertBadRequest(() -> service.update(PRODUCT_ID, request(true, GarmentType.DRESS, true, null, null)));
    }

    @Test
    void imagesOfAnotherProductAreRejected() {
        assertBadRequest(() -> service.update(PRODUCT_ID, request(true, GarmentType.DRESS, true, 999L, null)));
        assertBadRequest(() -> service.update(PRODUCT_ID, request(true, GarmentType.DRESS, true, 10L, List.of(999L))));
    }

    @Test
    void tooManyReferenceImagesAreRejected() {
        assertBadRequest(() -> service.update(PRODUCT_ID,
                request(true, GarmentType.DRESS, true, 10L, List.of(11L, 12L, 13L, 14L))));
    }

    @Test
    void primaryImageIsNotKeptAsReference() {
        when(configs.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        TryOnConfigResponse r = service.update(PRODUCT_ID,
                request(true, GarmentType.DRESS, true, 10L, List.of(10L, 11L)));
        assertEquals(List.of(11L), r.referenceImageIds());
    }

    @Test
    void draftSettingsCanBeSavedWithoutImages() {
        when(configs.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        TryOnConfigResponse r = service.update(PRODUCT_ID, request(false, null, false, null, null));
        assertFalse(r.enabled());
    }

    private Product product(long id, ProductStatus status, Long categoryId) {
        Product p = mock(Product.class);
        when(p.getId()).thenReturn(id);
        when(p.getStatus()).thenReturn(status);
        when(p.getCategoryId()).thenReturn(categoryId);
        when(p.getCreatedAt()).thenReturn(Instant.now());
        return p;
    }

    private ProductTryOnConfig config(long productId, boolean enabled, boolean ready, Long primary) {
        ProductTryOnConfig c = new ProductTryOnConfig(productId);
        c.setEnabled(enabled);
        c.setGarmentType(GarmentType.UPPER_BODY);
        c.setAiReady(ready);
        c.setPrimaryImageId(primary);
        return c;
    }

    @Test
    void listShowsOnlyPublishedEligibleProductsInTheCategory() {
        Product published = product(1L, ProductStatus.PUBLISHED, 5L);
        Product draft = product(2L, ProductStatus.DRAFT, 5L);
        Product otherCategory = product(3L, ProductStatus.PUBLISHED, 6L);
        when(configs.findByEnabledTrueAndAiReadyTrueAndPrimaryImageIdIsNotNull()).thenReturn(List.of(
                config(1L, true, true, 10L), config(2L, true, true, 10L), config(3L, true, true, 10L)));
        when(products.findAllById(any())).thenReturn(List.of(published, draft, otherCategory));
        ProductResponse response = mock(ProductResponse.class);
        when(response.getId()).thenReturn(1L);
        when(mapper.toPublicResponses(List.of(published))).thenReturn(List.of(response));

        List<TryOnProductResponse> result = service.listEligible(5L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).primaryImageId());
    }

    @Test
    void detailOfADisabledOrUnpublishedProductIs404() {
        when(configs.findById(1L)).thenReturn(Optional.of(config(1L, false, true, 10L)));
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.getEligible(1L)).getHttpStatus());

        when(configs.findById(2L)).thenReturn(Optional.of(config(2L, true, true, 10L)));
        Product archived = product(2L, ProductStatus.ARCHIVED, null);
        when(products.findById(2L)).thenReturn(Optional.of(archived));
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.getEligible(2L)).getHttpStatus());
    }
}
