package com.mayilboutique.order.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.core.dto.PageResponse;
import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.PaymentStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.order.dto.response.OrderResponse;
import com.mayilboutique.order.entity.Order;
import com.mayilboutique.order.entity.OrderItem;
import com.mayilboutique.order.repository.OrderItemRepository;
import com.mayilboutique.order.repository.OrderRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class AdminOrderService {

    private static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int MAX_PAGE_SIZE = 100;

    /** Forward-only fulfilment flow; cancellation is only possible before processing starts. */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.PACKED),
            OrderStatus.PACKED, Set.of(OrderStatus.SHIPPED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.RETURN_REQUESTED),
            OrderStatus.RETURN_REQUESTED, Set.of(OrderStatus.RETURNED),
            OrderStatus.RETURNED, Set.of(OrderStatus.REFUND_PENDING),
            OrderStatus.REFUND_PENDING, Set.of(OrderStatus.REFUNDED));

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;

    public AdminOrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                             OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderService = orderService;
    }

    /**
     * One page of orders, newest first. {@code q} matches order ref, customer mobile or name (case-insensitive,
     * partial); {@code from}/{@code to} are inclusive calendar days in the shop's time zone.
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> search(OrderStatus status, String q, LocalDate from, LocalDate to,
                                              int page, int size) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "'to' date cannot be before 'from' date");
        }
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderRef")), like),
                        cb.like(root.get("customerMobile"), like),
                        cb.like(cb.lower(root.get("customerName")), like)));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay(SHOP_ZONE).toInstant()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay(SHOP_ZONE).toInstant()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        Map<Long, List<OrderItem>> items = orders.isEmpty() ? Map.of()
                : orderItemRepository.findByOrderIdIn(orders.stream().map(Order::getId).toList()).stream()
                        .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return PageResponse.of(orders, o -> OrderResponse.forAdmin(o, items.getOrDefault(o.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(String orderRef) {
        Order order = orderRepository.findByOrderRef(orderRef).orElseThrow(AdminOrderService::notFound);
        return OrderResponse.forAdmin(order, orderItemRepository.findByOrderId(order.getId()));
    }

    @Transactional
    public OrderResponse updateStatus(String orderRef, OrderStatus next) {
        Order order = lock(orderRef);
        OrderStatus current = order.getStatus();
        if (current != next) {
            if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "Invalid order status transition from " + current + " to " + next);
            }
            if (next == OrderStatus.CANCELLED) {
                orderService.restoreStock(orderItemRepository.findByOrderId(order.getId()));
                orderService.releaseCoupon(order.getId());
            }
            if (next == OrderStatus.REFUNDED && order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
            if (next == OrderStatus.DELIVERED) {
                order.setDeliveredAt(java.time.Instant.now());
            }
            order.setStatus(next);
            order = orderRepository.save(order);
        }
        return OrderResponse.forAdmin(order, orderItemRepository.findByOrderId(order.getId()));
    }

    /** Cash on delivery has been collected: allowed once the parcel is shipped or delivered. */
    @Transactional
    public OrderResponse markPaid(String orderRef) {
        Order order = lock(orderRef);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Order is already marked as paid");
        }
        if (order.getPaymentStatus() != PaymentStatus.COD_PENDING
                || (order.getStatus() != OrderStatus.SHIPPED && order.getStatus() != OrderStatus.DELIVERED)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Only a shipped or delivered cash-on-delivery order awaiting payment can be marked as paid");
        }
        order.setPaymentStatus(PaymentStatus.PAID);
        order = orderRepository.save(order);
        return OrderResponse.forAdmin(order, orderItemRepository.findByOrderId(order.getId()));
    }

    /** Sets the staff-only note; a blank note clears it. Allowed in any order status. */
    @Transactional
    public OrderResponse updateNote(String orderRef, String note) {
        Order order = lock(orderRef);
        String trimmed = note == null ? "" : note.trim();
        order.setInternalNote(trimmed.isEmpty() ? null : trimmed);
        order = orderRepository.save(order);
        return OrderResponse.forAdmin(order, orderItemRepository.findByOrderId(order.getId()));
    }

    private Order lock(String orderRef) {
        return orderRepository.findByOrderRefForUpdate(orderRef).orElseThrow(AdminOrderService::notFound);
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "Order not found");
    }
}
