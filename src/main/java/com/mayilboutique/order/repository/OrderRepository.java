package com.mayilboutique.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mayilboutique.order.entity.Order;
import com.mayilboutique.core.enums.OrderStatus;

import jakarta.persistence.Temporal;
import java.time.Instant;

public interface OrderRepository extends JpaRepository<Order, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Order> {

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status <> :excludedStatus1 AND o.status <> :excludedStatus2")
    java.math.BigDecimal sumSalesExcluding(@Param("excludedStatus1") OrderStatus excludedStatus1,
                                           @Param("excludedStatus2") OrderStatus excludedStatus2);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status <> 'CANCELLED' AND o.status <> 'REFUNDED'")
    java.math.BigDecimal sumSalesExcludingCancelledAndRefunded();

    List<Order> findByOrderRefIsNotNullOrderByCreatedAtDesc();

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    java.util.Optional<Order> findByOrderRefAndUserId(String orderRef, Long userId);

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    java.util.Optional<Order> findByOrderRef(String orderRef);

    /** Row-locked lookup for admin changes, so two status updates cannot both act on the same state. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderRef = :orderRef")
    java.util.Optional<Order> findByOrderRefForUpdate(@Param("orderRef") String orderRef);

    boolean existsByOrderRef(String orderRef);
}
