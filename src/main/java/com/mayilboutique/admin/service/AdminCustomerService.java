package com.mayilboutique.admin.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.admin.dto.response.CustomerDetailResponse;
import com.mayilboutique.admin.dto.response.CustomerSummaryResponse;
import com.mayilboutique.auth.repository.UserRepository;
import com.mayilboutique.core.dto.PageResponse;
import com.mayilboutique.core.entity.User;
import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.Role;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.order.dto.response.OrderResponse;
import com.mayilboutique.order.entity.Order;
import com.mayilboutique.order.entity.OrderItem;
import com.mayilboutique.order.repository.OrderItemRepository;
import com.mayilboutique.order.repository.OrderRepository;

@Service
public class AdminCustomerService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public AdminCustomerService(UserRepository userRepository, OrderRepository orderRepository,
                                OrderItemRepository orderItemRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerSummaryResponse> search(String q, int page, int size) {
        String pattern = "%" + (q == null ? "" : q.trim()) + "%";
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.of(userRepository.searchCustomers(pattern, pageable), c -> c);
    }

    @Transactional(readOnly = true)
    public CustomerDetailResponse get(Long id) {
        User user = userRepository.findById(id)
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Customer not found"));
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(id);
        Map<Long, List<OrderItem>> items = orders.isEmpty() ? Map.of()
                : orderItemRepository.findByOrderIdIn(orders.stream().map(Order::getId).toList()).stream()
                        .collect(Collectors.groupingBy(OrderItem::getOrderId));

        BigDecimal spent = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.REFUNDED)
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant lastOrderAt = orders.isEmpty() ? null : orders.get(0).getCreatedAt();
        CustomerSummaryResponse summary = new CustomerSummaryResponse(user.getId(), user.getMobile(),
                user.getStatus(), user.getCreatedAt(), orders.size(), spent, lastOrderAt);

        return new CustomerDetailResponse(summary, orders.stream()
                .map(o -> OrderResponse.forAdmin(o, items.getOrDefault(o.getId(), List.of())))
                .toList());
    }
}
