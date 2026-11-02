package com.mayilboutique.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByCreatedAtAsc();

    boolean existsByCategoryId(Long categoryId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    List<Product> findAllByStatusOrderByCreatedAtAsc(ProductStatus status);

    List<Product> findByStatusAndCategoryIdOrderByCreatedAtAsc(ProductStatus status, Long categoryId);

    Optional<Product> findBySlug(String slug);
}
