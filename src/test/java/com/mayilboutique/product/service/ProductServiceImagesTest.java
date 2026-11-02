package com.mayilboutique.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.mayilboutique.category.repository.CategoryRepository;
import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.product.dto.request.ProductRequest;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductImage;
import com.mayilboutique.product.repository.ProductImageRepository;
import com.mayilboutique.product.repository.ProductRepository;

class ProductServiceImagesTest {

    private ProductRepository products;
    private ProductImageRepository images;
    private ProductImageService imageService;
    private ProductImageFiles files;
    private ProductResponseMapper mapper;
    private ProductService service;
    private final ProductRequest request =
            new ProductRequest("Kurta", "kurta", null, null, new BigDecimal("100"), null, "SKU-1");

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        images = mock(ProductImageRepository.class);
        imageService = mock(ProductImageService.class);
        files = mock(ProductImageFiles.class);
        mapper = mock(ProductResponseMapper.class);
        service = new ProductService(products, mock(CategoryRepository.class), images, imageService, files, mapper);
        when(products.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 1L);
            return p;
        });
    }

    private Product existing() {
        Product p = new Product("Kurta", "kurta", null, null, new BigDecimal("100"), null, "SKU-1",
                ProductStatus.DRAFT, 0);
        ReflectionTestUtils.setField(p, "id", 1L);
        when(products.findById(1L)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void createAttachesTheUploadedImagesToTheNewProduct() {
        List<org.springframework.web.multipart.MultipartFile> uploads =
                List.of(new MockMultipartFile("images", "a.png", "image/png", new byte[] {1}));

        service.createProduct(request, uploads);

        verify(imageService).attach(1L, uploads);
    }

    @Test
    void createWithoutImagesStillWorks() {
        service.createProduct(request);

        verify(imageService).attach(1L, List.of());
    }

    @Test
    void updateLeavesImagesAlone() {
        existing();

        service.updateProduct(1L, request);

        verify(images, never()).delete(any());
        verify(imageService, never()).attach(any(), any());
        verify(files, never()).discardIfUnused(any());
    }

    @Test
    void deleteRemovesImageRowsBeforeTheProductThenReleasesEachFile() {
        Product p = existing();
        ProductImage cover = new ProductImage(1L, "cover.png", 0);
        ProductImage second = new ProductImage(1L, "second.png", 1);
        when(images.findByProductIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(cover, second));

        service.deleteProduct(1L);

        InOrder order = inOrder(images, products, files);
        order.verify(images).deleteAll(List.of(cover, second));
        order.verify(products).delete(p);
        order.verify(products).flush();
        order.verify(files).discardIfUnused("cover.png");
        order.verify(files).discardIfUnused("second.png");
    }

    @Test
    void responsesUseTheMapperSoCoverAndImagesAreIncluded() {
        Product p = existing();
        service.getProduct(1L);

        verify(mapper).toResponse(p);
        assertEquals(1L, p.getId());
    }
}
