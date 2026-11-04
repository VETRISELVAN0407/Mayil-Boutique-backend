package com.mayilboutique.product.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mayilboutique.product.entity.ProductVariant;
import com.mayilboutique.stock.dto.LowStockResponse;

import jakarta.persistence.LockModeType;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findAllByProductIdOrderByCreatedAtAsc(Long productId);

    /** Row-locked lookup (SELECT ... FOR UPDATE) for read-modify-write stock changes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);

    /** Rows of {@code [productId, total stock of its variants]}; products without variants are absent. */
    @Query("SELECT v.productId, SUM(v.stockQuantity) FROM ProductVariant v WHERE v.productId IN :productIds GROUP BY v.productId")
    List<Object[]> sumStockByProductIds(@Param("productIds") Collection<Long> productIds);

    /** In-stock variants of the given products (used for the storefront size/colour filters). */
    List<ProductVariant> findByProductIdInAndStockQuantityGreaterThan(Collection<Long> productIds, Integer stockQuantity);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    /** Variants of published products at or below {@code threshold}, lowest stock first. */
    @Query(value = "SELECT new com.mayilboutique.stock.dto.LowStockResponse("
            + "v.id, p.id, p.name, v.sku, v.size, v.sizeNumber, v.colour, v.stockQuantity) "
            + "FROM ProductVariant v JOIN Product p ON p.id = v.productId "
            + "WHERE p.status = 'PUBLISHED' AND v.stockQuantity <= :threshold "
            + "ORDER BY v.stockQuantity ASC, p.name ASC, v.id ASC",
            countQuery = "SELECT COUNT(v) FROM ProductVariant v JOIN Product p ON p.id = v.productId "
                    + "WHERE p.status = 'PUBLISHED' AND v.stockQuantity <= :threshold")
    Page<LowStockResponse> findLowStock(@Param("threshold") int threshold, Pageable pageable);

    @Query("SELECT COUNT(v) FROM ProductVariant v JOIN Product p ON p.id = v.productId "
            + "WHERE p.status = 'PUBLISHED' AND v.stockQuantity <= :threshold")
    long countLowStock(@Param("threshold") int threshold);
}
