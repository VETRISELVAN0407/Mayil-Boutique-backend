package com.mayilboutique.order.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.order.entity.OrderItem;
import com.mayilboutique.product.entity.ProductVariant;
import com.mayilboutique.order.entity.Order;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);

    boolean existsByImageFilename(String imageFilename);

    /** The user's order lines for a product whose order is in one of {@code statuses}, newest order first. */
    @Query("SELECT oi FROM OrderItem oi, Order o, ProductVariant v "
            + "WHERE oi.orderId = o.id AND oi.variantId = v.id AND v.productId = :productId "
            + "AND o.userId = :userId AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<OrderItem> findPurchasedLines(@Param("userId") Long userId, @Param("productId") Long productId,
                                       @Param("statuses") Collection<OrderStatus> statuses);
}
