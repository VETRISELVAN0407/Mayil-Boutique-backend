package com.mayilboutique.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.mayilboutique.address.entity.CustomerAddress;
import com.mayilboutique.address.service.AddressService;
import com.mayilboutique.cart.entity.CartItem;
import com.mayilboutique.cart.repository.CartItemRepository;
import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.PaymentStatus;
import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.order.dto.response.OrderResponse;
import com.mayilboutique.order.entity.Order;
import com.mayilboutique.order.entity.OrderItem;
import com.mayilboutique.order.repository.OrderItemRepository;
import com.mayilboutique.order.repository.OrderRepository;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductVariant;
import com.mayilboutique.product.entity.ProductImage;
import com.mayilboutique.product.repository.ProductImageRepository;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.repository.ProductVariantRepository;

class OrderServiceTest {

    private static final Long USER_ID = 7L;

    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private CartItemRepository cartItemRepository;
    private ProductVariantRepository variantRepository;
    private ProductRepository productRepository;
    private AddressService addressService;
    private ProductImageRepository imageRepository;
    private com.mayilboutique.coupon.service.CouponService couponService;
    private OrderService service;

    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        cartItemRepository = mock(CartItemRepository.class);
        variantRepository = mock(ProductVariantRepository.class);
        productRepository = mock(ProductRepository.class);
        addressService = mock(AddressService.class);
        imageRepository = mock(ProductImageRepository.class);
        couponService = mock(com.mayilboutique.coupon.service.CouponService.class);
        service = new OrderService(orderRepository, orderItemRepository, cartItemRepository,
                variantRepository, productRepository, addressService, imageRepository, couponService,
                new com.mayilboutique.sale.service.SalePricingService(mock(com.mayilboutique.sale.repository.SaleRepository.class)));

        CustomerAddress address = new CustomerAddress();
        address.setFullName("Priya");
        address.setMobile("9876543210");
        address.setLine1("1 Main St");
        address.setCity("Chennai");
        address.setState("Tamil Nadu");
        address.setPincode("600001");
        when(addressService.find(USER_ID, 1L)).thenReturn(address);

        product = mock(Product.class);
        when(product.getId()).thenReturn(10L);
        when(product.getName()).thenReturn("Kurta");
        when(product.getStatus()).thenReturn(ProductStatus.PUBLISHED);
        when(product.getPrice()).thenReturn(new BigDecimal("1000.00"));
        when(product.getSalePrice()).thenReturn(new BigDecimal("800.00"));
        when(productRepository.findAllById(anyIterable())).thenReturn(List.of(product));

        variant = new ProductVariant(10L, "SKU-1", "M", "Red", 5);
        ReflectionTestUtils.setField(variant, "id", 100L);
        when(variantRepository.findAllById(anyIterable())).thenReturn(List.of(variant));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", 55L);
            return o;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void cartHolds(int quantity) {
        when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of(new CartItem(USER_ID, 100L, quantity)));
    }

    @Test
    void placeOrderUsesSalePriceDecrementsStockAndClearsCart() {
        cartHolds(2);

        OrderResponse response = service.placeOrder(USER_ID, 1L, null);

        assertEquals(new BigDecimal("1600.00"), response.total());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(PaymentStatus.COD_PENDING, response.paymentStatus());
        assertEquals(3, variant.getStockQuantity());
        verify(cartItemRepository).deleteAll(anyList());
    }

    @Test
    void placeOrderSnapshotsTheCoverImageOnEachLine() {
        cartHolds(1);
        // cover first, then a later image of the same product
        when(imageRepository.findByProductIdInOrderBySortOrderAscIdAsc(any()))
                .thenReturn(List.of(new ProductImage(10L, "cover.jpg", 0), new ProductImage(10L, "second.jpg", 1)));

        OrderResponse response = service.placeOrder(USER_ID, 1L, null);

        assertEquals("cover.jpg", response.items().get(0).imageFilename());
    }

    @Test
    void placeOrderWithoutAnyImageLeavesTheSnapshotEmpty() {
        cartHolds(1);

        OrderResponse response = service.placeOrder(USER_ID, 1L, null);

        assertNull(response.items().get(0).imageFilename());
    }

    @Test
    void placeOrderRejectsEmptyCart() {
        when(cartItemRepository.findByUserId(USER_ID)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.placeOrder(USER_ID, 1L, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void placeOrderRejectsInsufficientStockWithoutTouchingStockOrCart() {
        cartHolds(6);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.placeOrder(USER_ID, 1L, null));

        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        assertEquals(5, variant.getStockQuantity());
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAll(anyList());
    }

    @Test
    void placeOrderRejectsUnpublishedProduct() {
        when(product.getStatus()).thenReturn(ProductStatus.ARCHIVED);
        cartHolds(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.placeOrder(USER_ID, 1L, null));

        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    @Test
    void cancelRestoresStockAndMarksCancelled() {
        Order order = new Order("MB1", "9876543210", "Priya", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.TEN, OrderStatus.PENDING, PaymentStatus.COD_PENDING);
        ReflectionTestUtils.setField(order, "id", 55L);
        order.setUserId(USER_ID);
        when(orderRepository.findByOrderRefForUpdate("MB1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderId(55L))
                .thenReturn(List.of(new OrderItem(55L, 100L, "Kurta", "SKU-1", "M", "Red", BigDecimal.TEN, 2)));
        when(variantRepository.findById(100L)).thenReturn(Optional.of(variant));

        OrderResponse response = service.cancel(USER_ID, "MB1");

        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals(7, variant.getStockQuantity());
    }

    @Test
    void cancelRejectsShippedOrder() {
        Order order = new Order("MB2", "9876543210", "Priya", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.TEN, OrderStatus.SHIPPED, PaymentStatus.COD_PENDING);
        order.setUserId(USER_ID);
        when(orderRepository.findByOrderRefForUpdate("MB2")).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.cancel(USER_ID, "MB2"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void cancelTreatsAnotherCustomersOrderAsNotFound() {
        Order order = new Order("MB3", "9876543210", "Priya", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.TEN, OrderStatus.PENDING, PaymentStatus.COD_PENDING);
        order.setUserId(USER_ID + 1);
        when(orderRepository.findByOrderRefForUpdate("MB3")).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.cancel(USER_ID, "MB3"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        verify(orderRepository, never()).save(any());
    }

    private Order deliveredOrder(String ref, java.time.Instant deliveredAt) {
        Order order = new Order(ref, "9876543210", "Priya", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.TEN, OrderStatus.DELIVERED, PaymentStatus.PAID);
        ReflectionTestUtils.setField(order, "id", 66L);
        order.setUserId(USER_ID);
        order.setDeliveredAt(deliveredAt);
        when(orderRepository.findByOrderRefForUpdate(ref)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderId(66L)).thenReturn(List.of());
        return order;
    }

    @Test
    void returnWithinWindowMovesToReturnRequestedAndKeepsReason() {
        deliveredOrder("MB10", java.time.Instant.now().minus(java.time.Duration.ofDays(2)));

        OrderResponse response = service.requestReturn(USER_ID, "MB10", "  Wrong size ");

        assertEquals(OrderStatus.RETURN_REQUESTED, response.status());
        assertEquals("Wrong size", response.returnReason());
        verify(variantRepository, never()).save(any());
    }

    @Test
    void returnAfterWindowIsRejected() {
        deliveredOrder("MB11", java.time.Instant.now().minus(java.time.Duration.ofDays(8)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.requestReturn(USER_ID, "MB11", "Late"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void returnIsOnlyAllowedForDeliveredOrders() {
        Order order = deliveredOrder("MB12", java.time.Instant.now());
        order.setStatus(OrderStatus.SHIPPED);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.requestReturn(USER_ID, "MB12", "Early"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void returnTreatsAnotherCustomersOrderAsNotFound() {
        Order order = deliveredOrder("MB13", java.time.Instant.now());
        order.setUserId(USER_ID + 1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.requestReturn(USER_ID, "MB13", "Not mine"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        verify(orderRepository, never()).save(any());
    }
}
