package com.mayilboutique.admin.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.admin.dto.response.DashboardSummaryResponse;
import com.mayilboutique.admin.dto.response.OrderSummaryResponse;
import com.mayilboutique.order.entity.Order;
import com.mayilboutique.order.repository.OrderRepository;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.repository.ProductVariantRepository;
import com.mayilboutique.auth.repository.UserRepository;
import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.Role;

@Service
public class AdminDashboardService {

    public static final int LOW_STOCK_THRESHOLD = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;

    public AdminDashboardService(OrderRepository orderRepository,
                                 ProductRepository productRepository,
                                 UserRepository userRepository,
                                 ProductVariantRepository variantRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.variantRepository = variantRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        BigDecimal sales = orderRepository.sumSalesExcludingCancelledAndRefunded();
        long orderCount = orderRepository.count();
        long customerCount = userRepository.countByRole(Role.CUSTOMER);
        long productCount = productRepository.count();
        // Stock lives on variants; same rule as the inventory low-stock list.
        long lowStockCount = variantRepository.countLowStock(LOW_STOCK_THRESHOLD);

        List<OrderSummaryResponse> recentOrders = orderRepository.findByOrderRefIsNotNullOrderByCreatedAtDesc()
                .stream()
                .limit(10)
                .map(this::toSummary)
                .toList();

        return new DashboardSummaryResponse(orderCount, sales, customerCount, productCount, lowStockCount, recentOrders);
    }

    private OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getOrderRef(),
                order.getCustomerMobile(),
                order.getCustomerName(),
                order.getTotal(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getCreatedAt());
    }
}
