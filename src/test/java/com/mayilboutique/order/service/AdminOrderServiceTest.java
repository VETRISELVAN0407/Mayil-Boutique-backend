package com.mayilboutique.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.mayilboutique.core.dto.PageResponse;
import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.PaymentStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.order.dto.response.OrderResponse;
import com.mayilboutique.order.entity.Order;
import com.mayilboutique.order.entity.OrderItem;
import com.mayilboutique.order.repository.OrderItemRepository;
import com.mayilboutique.order.repository.OrderRepository;

class AdminOrderServiceTest {

    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private OrderService orderService;
    private AdminOrderService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        orderService = mock(OrderService.class);
        service = new AdminOrderService(orderRepository, orderItemRepository, orderService);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Order order(OrderStatus status, PaymentStatus payment) {
        Order o = new Order("MB20260101000001", "9876543210", "Priya", BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, status, payment);
        when(orderRepository.findByOrderRefForUpdate("MB20260101000001")).thenReturn(Optional.of(o));
        return o;
    }

    private BusinessException statusFails(OrderStatus next) {
        return assertThrows(BusinessException.class, () -> service.updateStatus("MB20260101000001", next));
    }

    @Test
    void validForwardTransitionIsApplied() {
        order(OrderStatus.PENDING, PaymentStatus.COD_PENDING);
        OrderResponse response = service.updateStatus("MB20260101000001", OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, response.status());
        verify(orderService, never()).restoreStock(any());
    }

    @Test
    void skippingStepsIsRejected() {
        order(OrderStatus.PENDING, PaymentStatus.COD_PENDING);
        assertEquals(HttpStatus.BAD_REQUEST, statusFails(OrderStatus.SHIPPED).getHttpStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelledOrderCannotBeReopenedAndShippedCannotBeCancelled() {
        order(OrderStatus.CANCELLED, PaymentStatus.COD_PENDING);
        assertEquals(HttpStatus.BAD_REQUEST, statusFails(OrderStatus.CONFIRMED).getHttpStatus());
        order(OrderStatus.SHIPPED, PaymentStatus.COD_PENDING);
        assertEquals(HttpStatus.BAD_REQUEST, statusFails(OrderStatus.CANCELLED).getHttpStatus());
    }

    @Test
    void cancellingRestoresStock() {
        Order o = order(OrderStatus.CONFIRMED, PaymentStatus.COD_PENDING);
        List<OrderItem> items = List.of(new OrderItem(1L, 5L, "Kurta", "SKU-1", "M", null, BigDecimal.TEN, 2));
        when(orderItemRepository.findByOrderId(o.getId())).thenReturn(items);
        service.updateStatus("MB20260101000001", OrderStatus.CANCELLED);
        verify(orderService).restoreStock(items);
    }

    @Test
    void unknownOrderIsNotFound() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateStatus("nope", OrderStatus.CONFIRMED));
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void markPaidWorksForShippedCodOrder() {
        order(OrderStatus.SHIPPED, PaymentStatus.COD_PENDING);
        assertEquals(PaymentStatus.PAID, service.markPaid("MB20260101000001").paymentStatus());
    }

    @Test
    void markPaidRejectsAlreadyPaidAndNotYetShipped() {
        order(OrderStatus.DELIVERED, PaymentStatus.PAID);
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(BusinessException.class, () -> service.markPaid("MB20260101000001")).getHttpStatus());
        order(OrderStatus.PENDING, PaymentStatus.COD_PENDING);
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(BusinessException.class, () -> service.markPaid("MB20260101000001")).getHttpStatus());
    }

    @Test
    void refundingPaidOrderMarksPaymentRefunded() {
        order(OrderStatus.REFUND_PENDING, PaymentStatus.PAID);
        OrderResponse response = service.updateStatus("MB20260101000001", OrderStatus.REFUNDED);
        assertEquals(PaymentStatus.REFUNDED, response.paymentStatus());
    }

    @Test
    void noteIsTrimmedAndStored() {
        Order o = order(OrderStatus.PENDING, PaymentStatus.COD_PENDING);

        OrderResponse response = service.updateNote("MB20260101000001", "  gift wrap, call first  ");

        assertEquals("gift wrap, call first", o.getInternalNote());
        assertEquals("gift wrap, call first", response.internalNote());
    }

    @Test
    void blankNoteClearsIt() {
        Order o = order(OrderStatus.PENDING, PaymentStatus.COD_PENDING);
        o.setInternalNote("old note");

        OrderResponse response = service.updateNote("MB20260101000001", "   ");

        assertNull(o.getInternalNote());
        assertNull(response.internalNote());
    }

    @Test
    void noteCanBeEditedOnAnyStatusWithoutChangingIt() {
        Order o = order(OrderStatus.DELIVERED, PaymentStatus.PAID);

        service.updateNote("MB20260101000001", "customer happy");

        assertEquals(OrderStatus.DELIVERED, o.getStatus());
        assertEquals(PaymentStatus.PAID, o.getPaymentStatus());
    }

    @Test
    void searchRejectsRangeEndingBeforeItStarts() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.search(null, null, LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 24), 0, 20));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verify(orderRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void searchClampsPageSizeAndSortsNewestFirst() {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        PageResponse<OrderResponse> result = service.search(null, "priya", null, null, -3, 9999);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(any(Specification.class), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("createdAt").getDirection());
        assertEquals(0, result.totalElements());
        verify(orderItemRepository, never()).findByOrderIdIn(any());
    }

    @Test
    void searchReturnsOrdersWithTheirItems() {
        Order o = order(OrderStatus.PENDING, PaymentStatus.COD_PENDING);
        ReflectionTestUtils.setField(o, "id", 9L);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(o), PageRequest.of(0, 20), 1));
        when(orderItemRepository.findByOrderIdIn(List.of(9L)))
                .thenReturn(List.of(new OrderItem(9L, 100L, "Kurta", "SKU-1", "M", "Red", BigDecimal.TEN, 2)));

        PageResponse<OrderResponse> result = service.search(OrderStatus.PENDING, null, null, null, 0, 20);

        assertEquals(1, result.totalElements());
        assertEquals(1, result.content().get(0).items().size());
    }
}
