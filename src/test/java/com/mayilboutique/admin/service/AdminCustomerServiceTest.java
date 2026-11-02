package com.mayilboutique.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.mayilboutique.admin.dto.response.CustomerDetailResponse;
import com.mayilboutique.auth.repository.UserRepository;
import com.mayilboutique.core.entity.User;
import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.PaymentStatus;
import com.mayilboutique.core.enums.Role;
import com.mayilboutique.core.enums.UserStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.order.entity.Order;
import com.mayilboutique.order.repository.OrderItemRepository;
import com.mayilboutique.order.repository.OrderRepository;

class AdminCustomerServiceTest {

    private UserRepository userRepository;
    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private AdminCustomerService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        service = new AdminCustomerService(userRepository, orderRepository, orderItemRepository);
    }

    private User user(long id, Role role) {
        User u = new User("9000000077", "hash", role, UserStatus.ACTIVE);
        u.setId(id);
        return u;
    }

    private Order order(long id, BigDecimal total, OrderStatus status, Instant createdAt) {
        Order o = new Order("MB" + id, "9000000077", "Priya", total, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, total, status, PaymentStatus.COD_PENDING);
        ReflectionTestUtils.setField(o, "id", id);
        ReflectionTestUtils.setField(o, "createdAt", createdAt);
        return o;
    }

    @Test
    void detailTotalsExcludeCancelledAndRefundedOrders() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.CUSTOMER)));
        Instant newest = Instant.parse("2026-10-25T10:00:00Z");
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(
                order(3L, new BigDecimal("500.00"), OrderStatus.DELIVERED, newest),
                order(2L, new BigDecimal("900.00"), OrderStatus.CANCELLED, newest.minusSeconds(60)),
                order(1L, new BigDecimal("300.00"), OrderStatus.REFUNDED, newest.minusSeconds(120))));
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(List.of());

        CustomerDetailResponse detail = service.get(2L);

        assertEquals(3, detail.customer().orderCount());
        assertEquals(0, new BigDecimal("500.00").compareTo(detail.customer().totalSpent()));
        assertEquals(newest, detail.customer().lastOrderAt());
        assertEquals(3, detail.orders().size());
    }

    @Test
    void detailForCustomerWithoutOrdersHasZeroTotals() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.CUSTOMER)));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());

        CustomerDetailResponse detail = service.get(2L);

        assertEquals(0, detail.customer().orderCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(detail.customer().totalSpent()));
        assertNull(detail.customer().lastOrderAt());
    }

    @Test
    void detailTreatsAnAdminAccountAsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.ADMIN)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(1L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void searchWrapsTermInLikePatternAndClampsPageSize() {
        when(userRepository.searchCustomers(eq("%0077%"), any(Pageable.class))).thenReturn(Page.empty());

        service.search("  0077 ", -1, 5000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).searchCustomers(eq("%0077%"), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
    }
}
