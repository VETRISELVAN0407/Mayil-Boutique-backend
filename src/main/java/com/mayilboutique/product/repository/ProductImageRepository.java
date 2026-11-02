package com.mayilboutique.product.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mayilboutique.product.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /** Cover first. */
    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    List<ProductImage> findByProductIdInOrderBySortOrderAscIdAsc(Collection<Long> productIds);

    Optional<ProductImage> findFirstByProductIdOrderBySortOrderAscIdAsc(Long productId);

    Optional<ProductImage> findByIdAndProductId(Long id, Long productId);

    long countByProductId(Long productId);

    boolean existsByFilename(String filename);

    @Query("SELECT COALESCE(MAX(i.sortOrder), -1) FROM ProductImage i WHERE i.productId = :productId")
    int maxSortOrder(@Param("productId") Long productId);
}
