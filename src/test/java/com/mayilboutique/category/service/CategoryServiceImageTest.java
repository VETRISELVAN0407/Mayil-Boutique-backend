package com.mayilboutique.category.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.mayilboutique.category.dto.response.CategoryResponse;
import com.mayilboutique.category.entity.Category;
import com.mayilboutique.category.repository.CategoryRepository;
import com.mayilboutique.core.enums.CategoryStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.service.ProductImageFiles;

class CategoryServiceImageTest {

    private CategoryRepository categories;
    private ProductRepository products;
    private ProductImageFiles files;
    private CategoryService service;
    private Category category;
    private final MockMultipartFile upload = new MockMultipartFile("image", "a.png", "image/png", new byte[] {1});

    @BeforeEach
    void setUp() {
        categories = mock(CategoryRepository.class);
        products = mock(ProductRepository.class);
        files = mock(ProductImageFiles.class);
        service = new CategoryService(categories, products, files);
        category = new Category("Sarees", "sarees", null, null, CategoryStatus.ACTIVE);
        when(categories.findById(1L)).thenReturn(Optional.of(category));
    }

    @Test
    void setImageStoresTheFileAndDiscardsThePreviousOne() {
        category.setImageFilename("old.png");
        when(files.store(upload)).thenReturn("new.png");

        CategoryResponse response = service.setImage(1L, upload);

        assertEquals("new.png", response.getImageFilename());
        verify(categories).saveAndFlush(category);
        verify(files).discardIfUnused("old.png");
    }

    @Test
    void removeImageClearsTheFilenameAndDiscardsTheFile() {
        category.setImageFilename("old.png");

        CategoryResponse response = service.removeImage(1L);

        assertNull(response.getImageFilename());
        verify(files).discardIfUnused("old.png");
    }

    @Test
    void unknownCategoryIs404AndNothingIsStored() {
        when(categories.findById(2L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.setImage(2L, upload));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        verify(files, never()).store(any());
    }

    @Test
    void deletingACategoryDiscardsItsPicture() {
        category.setImageFilename("old.png");

        service.deleteCategory(1L);

        verify(categories).delete(category);
        verify(files).discardIfUnused("old.png");
    }
}
