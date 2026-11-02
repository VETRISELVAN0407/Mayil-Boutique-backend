package com.mayilboutique.product.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mayilboutique.category.repository.CategoryRepository;
import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.dto.request.ProductRequest;
import com.mayilboutique.product.dto.response.ProductResponse;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductImage;
import com.mayilboutique.product.repository.ProductImageRepository;
import com.mayilboutique.product.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository imageRepository;
    private final ProductImageService imageService;
    private final ProductImageFiles imageFiles;
    private final ProductResponseMapper mapper;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          ProductImageRepository imageRepository, ProductImageService imageService,
                          ProductImageFiles imageFiles, ProductResponseMapper mapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.imageRepository = imageRepository;
        this.imageService = imageService;
        this.imageFiles = imageFiles;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts() {
        return mapper.toResponses(productRepository.findAllByOrderByCreatedAtAsc());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return mapper.toResponse(getProductOrThrow(id));
    }

    public ProductResponse createProduct(ProductRequest request) {
        return createProduct(request, List.of());
    }

    /** {@code images} are optional; the first becomes the cover. More can be added later. */
    @Transactional
    public ProductResponse createProduct(ProductRequest request, List<MultipartFile> images) {
        if (productRepository.existsBySlug(request.getSlug())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Product slug already exists");
        }
        if (productRepository.existsBySku(request.getSku())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Product SKU already exists");
        }
        if (request.getCategoryId() != null && !categoryRepository.existsById(request.getCategoryId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Category not found");
        }
        Product product = productRepository.save(new Product(request.getName(), request.getSlug(),
                request.getDescription(), request.getCategoryId(), request.getPrice(), request.getSalePrice(),
                request.getSku(), ProductStatus.DRAFT, 0));
        imageService.attach(product.getId(), images);
        return mapper.toResponse(product);
    }

    /** Product details only; images are managed through {@link ProductImageService}. */
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = getProductOrThrow(id);
        if (productRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Product slug already exists");
        }
        if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Product SKU already exists");
        }
        if (request.getCategoryId() != null && !categoryRepository.existsById(request.getCategoryId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Category not found");
        }
        if (request.getSalePrice() != null && request.getSalePrice().compareTo(request.getPrice()) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Sale price cannot be greater than the product price");
        }
        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setDescription(request.getDescription());
        product.setCategoryId(request.getCategoryId());
        product.setPrice(request.getPrice());
        product.setSalePrice(request.getSalePrice());
        product.setSku(request.getSku());
        return mapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStatus(Long id, ProductStatus status) {
        Product product = getProductOrThrow(id);
        validateStatusTransition(product.getStatus(), status);
        product.setStatus(status);
        return mapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductOrThrow(id);
        List<ProductImage> images = imageRepository.findByProductIdOrderBySortOrderAscIdAsc(id);
        imageRepository.deleteAll(images);
        productRepository.delete(product);
        productRepository.flush();
        // Files that past orders still show are kept.
        images.forEach(image -> imageFiles.discardIfUnused(image.getFilename()));
    }

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void validateStatusTransition(ProductStatus currentStatus, ProductStatus nextStatus) {
        if (currentStatus == nextStatus) {
            return;
        }
        boolean validTransition = switch (currentStatus) {
            case DRAFT -> nextStatus == ProductStatus.PUBLISHED;
            case PUBLISHED -> nextStatus == ProductStatus.ARCHIVED || nextStatus == ProductStatus.DRAFT;
            case ARCHIVED -> nextStatus == ProductStatus.DRAFT;
        };
        if (!validTransition) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Invalid product status transition from " + currentStatus + " to " + nextStatus);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getPublishedProducts(Long categoryId, String search) {
        List<Product> products = categoryId != null
                ? productRepository.findByStatusAndCategoryIdOrderByCreatedAtAsc(ProductStatus.PUBLISHED, categoryId)
                : productRepository.findAllByStatusOrderByCreatedAtAsc(ProductStatus.PUBLISHED);
        if (search != null && !search.isBlank()) {
            String term = search.trim().toLowerCase();
            products = products.stream()
                    .filter(product -> product.getName() != null && product.getName().toLowerCase().contains(term))
                    .toList();
        }
        return mapper.toPublicResponses(products);
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublishedProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Product not found"));
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return mapper.toPublicResponse(product);
    }
}
