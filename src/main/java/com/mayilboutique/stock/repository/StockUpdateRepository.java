package com.mayilboutique.stock.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mayilboutique.stock.dto.StockUpdateResponse;
import com.mayilboutique.stock.entity.StockUpdate;

public interface StockUpdateRepository extends JpaRepository<StockUpdate, Long> {

    @Query(value = "SELECT new com.mayilboutique.stock.dto.StockUpdateResponse("
            + "s.id, s.productId, p.name, s.variantId, v.sku, v.size, v.colour, s.stockAdded, s.totalStock, s.updatedOn) "
            + "FROM StockUpdate s JOIN Product p ON p.id = s.productId JOIN ProductVariant v ON v.id = s.variantId "
            + "ORDER BY s.updatedOn DESC, s.id DESC",
            countQuery = "SELECT COUNT(s) FROM StockUpdate s")
    Page<StockUpdateResponse> findHistory(Pageable pageable);

    @Query(value = "SELECT new com.mayilboutique.stock.dto.StockUpdateResponse("
            + "s.id, s.productId, p.name, s.variantId, v.sku, v.size, v.colour, s.stockAdded, s.totalStock, s.updatedOn) "
            + "FROM StockUpdate s JOIN Product p ON p.id = s.productId JOIN ProductVariant v ON v.id = s.variantId "
            + "WHERE s.variantId = :variantId ORDER BY s.updatedOn DESC, s.id DESC",
            countQuery = "SELECT COUNT(s) FROM StockUpdate s WHERE s.variantId = :variantId")
    Page<StockUpdateResponse> findHistoryByVariant(@Param("variantId") Long variantId, Pageable pageable);
}
