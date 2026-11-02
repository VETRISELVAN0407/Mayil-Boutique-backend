package com.mayilboutique.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductImage;
import com.mayilboutique.product.repository.ProductImageRepository;
import com.mayilboutique.product.repository.ProductRepository;

class ProductImageServiceTest {

    private static final Long PRODUCT_ID = 1L;

    private ProductRepository products;
    private ProductImageRepository images;
    private ProductImageFiles files;
    private ProductResponseMapper mapper;
    private ProductImageService service;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        images = mock(ProductImageRepository.class);
        files = mock(ProductImageFiles.class);
        mapper = mock(ProductResponseMapper.class);
        service = new ProductImageService(products, images, files, mapper);
        when(products.findById(PRODUCT_ID)).thenReturn(Optional.of(mock(Product.class)));
    }

    private MockMultipartFile upload(String name) {
        return new MockMultipartFile("images", name, "image/png", new byte[] {1});
    }

    private ProductImage image(long id, String filename, int order) {
        ProductImage i = new ProductImage(PRODUCT_ID, filename, order);
        ReflectionTestUtils.setField(i, "id", id);
        return i;
    }

    @Test
    void addAppendsAfterTheExistingImagesInUploadOrder() {
        MockMultipartFile first = upload("a.png");
        MockMultipartFile second = upload("b.png");
        when(images.countByProductId(PRODUCT_ID)).thenReturn(2L);
        when(images.maxSortOrder(PRODUCT_ID)).thenReturn(1);
        when(files.store(first)).thenReturn("a.png");
        when(files.store(second)).thenReturn("b.png");

        service.add(PRODUCT_ID, List.of(first, second));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductImage>> saved = ArgumentCaptor.forClass(List.class);
        verify(images).saveAll(saved.capture());
        assertEquals(List.of("a.png", "b.png"), saved.getValue().stream().map(ProductImage::getFilename).toList());
        assertEquals(List.of(2, 3), saved.getValue().stream().map(ProductImage::getSortOrder).toList());
    }

    @Test
    void firstImageOfAProductBecomesTheCover() {
        MockMultipartFile only = upload("a.png");
        when(images.countByProductId(PRODUCT_ID)).thenReturn(0L);
        when(images.maxSortOrder(PRODUCT_ID)).thenReturn(-1);
        when(files.store(only)).thenReturn("a.png");

        service.add(PRODUCT_ID, List.of(only));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductImage>> saved = ArgumentCaptor.forClass(List.class);
        verify(images).saveAll(saved.capture());
        assertEquals(0, saved.getValue().get(0).getSortOrder());
    }

    @Test
    void addRejectsMoreThanTheLimitWithoutStoringAnything() {
        when(images.countByProductId(PRODUCT_ID)).thenReturn((long) ProductImageService.MAX_IMAGES);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.add(PRODUCT_ID, List.of(upload("a.png"))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verify(files, never()).store(any());
        verify(images, never()).saveAll(anyList());
    }

    @Test
    void addRejectsARequestWithNoImages() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.add(PRODUCT_ID, List.of()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void addToAnUnknownProductIsNotFound() {
        when(products.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.add(99L, List.of(upload("a.png"))));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void attachWithNothingToAddDoesNothing() {
        service.attach(PRODUCT_ID, null);
        service.attach(PRODUCT_ID, List.of());

        verify(images, never()).saveAll(anyList());
    }

    @Test
    void deleteRemovesTheRowAndLetsTheFileLifecycleDecideAboutTheFile() {
        ProductImage target = image(7L, "old.png", 1);
        when(images.findByIdAndProductId(7L, PRODUCT_ID)).thenReturn(Optional.of(target));

        service.delete(PRODUCT_ID, 7L);

        verify(images).delete(target);
        verify(files).discardIfUnused("old.png");
    }

    @Test
    void deleteOfAnImageOfAnotherProductIsNotFound() {
        when(images.findByIdAndProductId(7L, PRODUCT_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(PRODUCT_ID, 7L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        verify(files, never()).discardIfUnused(any());
    }

    @Test
    void reorderMakesTheFirstIdTheCover() {
        ProductImage a = image(1L, "a.png", 0);
        ProductImage b = image(2L, "b.png", 1);
        ProductImage c = image(3L, "c.png", 2);
        when(images.findByProductIdOrderBySortOrderAscIdAsc(PRODUCT_ID)).thenReturn(List.of(a, b, c));

        service.reorder(PRODUCT_ID, List.of(3L, 1L, 2L));

        assertEquals(0, c.getSortOrder());
        assertEquals(1, a.getSortOrder());
        assertEquals(2, b.getSortOrder());
        verify(images).saveAll(anyList());
    }

    @Test
    void reorderRejectsAMissingDuplicatedOrForeignId() {
        when(images.findByProductIdOrderBySortOrderAscIdAsc(PRODUCT_ID))
                .thenReturn(List.of(image(1L, "a.png", 0), image(2L, "b.png", 1)));

        for (List<Long> bad : List.of(List.of(1L), List.of(1L, 1L), List.of(1L, 99L), List.of(1L, 2L, 3L))) {
            BusinessException ex = assertThrows(BusinessException.class, () -> service.reorder(PRODUCT_ID, bad));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        }
        verify(images, never()).saveAll(anyList());
    }
}
