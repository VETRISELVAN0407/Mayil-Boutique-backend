package com.mayilboutique.category.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mayilboutique.category.dto.request.CategoryRequest;
import com.mayilboutique.category.dto.response.CategoryResponse;
import com.mayilboutique.category.entity.Category;
import com.mayilboutique.category.repository.CategoryRepository;
import com.mayilboutique.core.enums.CategoryStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.service.ProductImageFiles;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageFiles imageFiles;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository,
                           ProductImageFiles imageFiles) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.imageFiles = imageFiles;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderByCreatedAtAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findAllByStatusOrderByCreatedAtAsc(CategoryStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long id) {
        return toResponse(getCategoryOrThrow(id));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Category slug already exists");
        }
        Category category = new Category(request.getName(), request.getSlug(), request.getDescription(), request.getImage(),
                CategoryStatus.ACTIVE);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = getCategoryOrThrow(id);
        if (categoryRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Category slug already exists");
        }
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setImage(request.getImage());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateStatus(Long id, CategoryStatus status) {
        Category category = getCategoryOrThrow(id);
        category.setStatus(status);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryOrThrow(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Category cannot be deleted because products are associated with it");
        }
        categoryRepository.delete(category);
        categoryRepository.flush();
        imageFiles.discardIfUnused(category.getImageFilename());
    }

    /** Sets (or replaces) the uploaded picture; the previous file is removed once nothing else uses it. */
    @Transactional
    public CategoryResponse setImage(Long id, MultipartFile upload) {
        Category category = getCategoryOrThrow(id);
        String previous = category.getImageFilename();
        category.setImageFilename(imageFiles.store(upload));
        categoryRepository.saveAndFlush(category);
        imageFiles.discardIfUnused(previous);
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse removeImage(Long id) {
        Category category = getCategoryOrThrow(id);
        String previous = category.getImageFilename();
        category.setImageFilename(null);
        categoryRepository.saveAndFlush(category);
        imageFiles.discardIfUnused(previous);
        return toResponse(category);
    }

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getDescription(),
                category.getImage(), category.getImageFilename(), category.getStatus(), category.getCreatedAt(), category.getUpdatedAt());
    }
}
