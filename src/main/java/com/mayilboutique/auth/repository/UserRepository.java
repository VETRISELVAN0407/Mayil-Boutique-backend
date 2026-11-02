package com.mayilboutique.auth.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mayilboutique.admin.dto.response.CustomerSummaryResponse;

import com.mayilboutique.core.entity.User;
import com.mayilboutique.core.enums.Role;
import com.mayilboutique.auth.entity.RefreshToken;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMobile(String mobile);

    long countByRole(Role role);

    /** Customers with their order totals; {@code q} is a ready-made LIKE pattern on the mobile number. */
    @Query(value = "SELECT new com.mayilboutique.admin.dto.response.CustomerSummaryResponse("
            + "u.id, u.mobile, u.status, u.createdAt, COUNT(o.id), "
            + "COALESCE(SUM(CASE WHEN o.status <> 'CANCELLED' AND o.status <> 'REFUNDED' THEN o.total END), 0), "
            + "MAX(o.createdAt)) "
            + "FROM User u LEFT JOIN Order o ON o.userId = u.id "
            + "WHERE u.role = 'CUSTOMER' AND u.mobile LIKE :q "
            + "GROUP BY u.id, u.mobile, u.status, u.createdAt "
            + "ORDER BY u.createdAt DESC, u.id DESC",
            countQuery = "SELECT COUNT(u) FROM User u WHERE u.role = 'CUSTOMER' AND u.mobile LIKE :q")
    Page<CustomerSummaryResponse> searchCustomers(@Param("q") String q, Pageable pageable);
}
