package com.mayilboutique.product.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.product.dto.response.ProductResponse;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductImage;
import com.mayilboutique.product.repository.ProductImageRepository;
import com.mayilboutique.product.repository.ProductRepository;

/** Adds, removes and orders a product's images. The first image is the cover. */
@Service
public class ProductImageService {

    public static final int MAX_IMAGES = 8;

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final ProductImageFiles files;
    private final ProductResponseMapper mapper;

    public ProductImageService(ProductRepository productRepository, ProductImageRepository imageRepository,
                               ProductImageFiles files, ProductResponseMapper mapper) {
        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
        this.files = files;
        this.mapper = mapper;
    }

    @Transactional
    public ProductResponse add(Long productId, List<MultipartFile> uploads) {
        Product product = find(productId);
        if (present(uploads).isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "No image provided");
        }
        attach(productId, uploads);
        return mapper.toResponse(product);
    }

    /** Appends the uploads after the existing images. Must run inside a transaction so files follow its outcome. */
    void attach(Long productId, List<MultipartFile> uploads) {
        List<MultipartFile> given = present(uploads);
        if (given.isEmpty()) {
            return;
        }
        if (imageRepository.countByProductId(productId) + given.size() > MAX_IMAGES) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "A product can have at most " + MAX_IMAGES + " images");
        }
        int next = imageRepository.maxSortOrder(productId) + 1;
        List<ProductImage> rows = new ArrayList<>();
        for (MultipartFile upload : given) {
            rows.add(new ProductImage(productId, files.store(upload), next++));
        }
        imageRepository.saveAll(rows);
    }

    @Transactional
    public ProductResponse delete(Long productId, Long imageId) {
        Product product = find(productId);
        ProductImage image = imageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Image not found"));
        imageRepository.delete(image);
        files.discardIfUnused(image.getFilename());
        return mapper.toResponse(product);
    }

    /** {@code imageIds} must list each of the product's images exactly once; the first becomes the cover. */
    @Transactional
    public ProductResponse reorder(Long productId, List<Long> imageIds) {
        Product product = find(productId);
        List<ProductImage> images = imageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId);
        Map<Long, ProductImage> byId = images.stream().collect(Collectors.toMap(ProductImage::getId, Function.identity()));
        if (imageIds == null || imageIds.size() != images.size() || !new HashSet<>(imageIds).equals(byId.keySet())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Image order must list each of the product's images exactly once");
        }
        for (int i = 0; i < imageIds.size(); i++) {
            byId.get(imageIds.get(i)).setSortOrder(i);
        }
        imageRepository.saveAll(images);
        return mapper.toResponse(product);
    }

    private static List<MultipartFile> present(List<MultipartFile> uploads) {
        return uploads == null ? List.of() : uploads.stream().filter(Objects::nonNull).toList();
    }

    private Product find(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
