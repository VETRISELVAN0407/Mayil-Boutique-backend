package com.mayilboutique.order.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.address.entity.CustomerAddress;
import com.mayilboutique.address.service.AddressService;
import com.mayilboutique.cart.entity.CartItem;
import com.mayilboutique.cart.repository.CartItemRepository;
import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.core.enums.PaymentMethod;
import com.mayilboutique.core.enums.PaymentStatus;
import com.mayilboutique.core.enums.ProductStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.coupon.service.CouponService;
import com.mayilboutique.order.dto.response.OrderResponse;
import com.mayilboutique.order.entity.Order;
import com.mayilboutique.order.entity.OrderItem;
import com.mayilboutique.order.repository.OrderItemRepository;
import com.mayilboutique.order.repository.OrderRepository;
import com.mayilboutique.product.entity.Product;
import com.mayilboutique.product.entity.ProductImage;
import com.mayilboutique.product.entity.ProductVariant;
import com.mayilboutique.product.repository.ProductImageRepository;
import com.mayilboutique.product.repository.ProductRepository;
import com.mayilboutique.product.repository.ProductVariantRepository;
import com.mayilboutique.sale.service.SalePricingService;

@Service
public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final AddressService addressService;
    private final ProductImageRepository productImageRepository;
    private final CouponService couponService;
    private final SalePricingService pricing;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartItemRepository cartItemRepository,
                        ProductVariantRepository variantRepository,
                        ProductRepository productRepository,
                        AddressService addressService,
                        ProductImageRepository productImageRepository,
                        CouponService couponService,
                        SalePricingService pricing) {
        this.productImageRepository = productImageRepository;
        this.couponService = couponService;
        this.pricing = pricing;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.addressService = addressService;
    }

    @Transactional
    public OrderResponse placeOrder(Long userId, Long addressId, String couponCode) {
        CustomerAddress address = addressService.find(userId, addressId);

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        Map<Long, ProductVariant> variants = variantRepository
                .findAllById(cartItems.stream().map(CartItem::getVariantId).toList()).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        Map<Long, Product> products = productRepository
                .findAllById(variants.values().stream().map(ProductVariant::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        // Images come back cover-first, so the first one seen for a product is its cover.
        Map<Long, String> covers = productImageRepository
                .findByProductIdInOrderBySortOrderAscIdAsc(products.keySet()).stream()
                .collect(Collectors.toMap(ProductImage::getProductId, ProductImage::getFilename, (first, later) -> first));

        Map<Long, SalePricingService.PriceInfo> prices = pricing.priceInfo(products.values());
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> lines = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            ProductVariant variant = variants.get(cartItem.getVariantId());
            Product product = variant == null ? null : products.get(variant.getProductId());
            if (variant == null || product == null || product.getStatus() != ProductStatus.PUBLISHED) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "An item in your cart is no longer available. Please review your cart.");
            }
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "Insufficient stock for " + product.getName() + " (" + variant.getSku() + ")");
            }
            variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());

            BigDecimal unitPrice = SalePricingService.unitPrice(product, prices.get(product.getId()));
            OrderItem line = new OrderItem(null, variant.getId(), product.getName(), variant.getSku(),
                    variant.getSize(), variant.getColour(), unitPrice, cartItem.getQuantity(),
                    covers.get(product.getId()));
            subtotal = subtotal.add(line.getLineTotal());
            lines.add(line);
        }
        variantRepository.saveAll(variants.values());

        CouponService.Applied coupon = couponCode == null || couponCode.isBlank() ? null
                : couponService.lockAndEvaluate(userId, couponCode, subtotal);
        BigDecimal discount = coupon == null ? BigDecimal.ZERO : coupon.discount();

        // Shipping and tax are not configured yet; the total is the item subtotal minus any coupon.
        Order order = new Order(generateOrderRef(), address.getMobile(), address.getFullName(), subtotal,
                discount, BigDecimal.ZERO, BigDecimal.ZERO, subtotal.subtract(discount),
                OrderStatus.PENDING, PaymentStatus.COD_PENDING);
        if (coupon != null) {
            order.setCouponCode(coupon.coupon().getCode());
        }
        order.setUserId(userId);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setShipName(address.getFullName());
        order.setShipMobile(address.getMobile());
        order.setShipLine1(address.getLine1());
        order.setShipLine2(address.getLine2());
        order.setShipCity(address.getCity());
        order.setShipState(address.getState());
        order.setShipPincode(address.getPincode());
        order = orderRepository.save(order);
        if (coupon != null) {
            couponService.recordRedemption(coupon, userId, order.getId());
        }

        List<OrderItem> saved = new ArrayList<>();
        for (OrderItem l : lines) {
            saved.add(new OrderItem(order.getId(), l.getVariantId(), l.getProductName(), l.getSku(), l.getSize(),
                    l.getColour(), l.getUnitPrice(), l.getQuantity(), l.getImageFilename()));
        }
        saved = orderItemRepository.saveAll(saved);

        cartItemRepository.deleteAll(cartItems);
        return OrderResponse.from(order, saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, List<OrderItem>> items = orderItemRepository
                .findByOrderIdIn(orders.stream().map(Order::getId).toList()).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return orders.stream()
                .map(o -> OrderResponse.from(o, items.getOrDefault(o.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long userId, String orderRef) {
        Order order = find(userId, orderRef);
        return OrderResponse.from(order, orderItemRepository.findByOrderId(order.getId()));
    }

    @Transactional
    public OrderResponse cancel(Long userId, String orderRef) {
        // Row-locked like the admin path, so a simultaneous admin cancel cannot restore the stock twice.
        Order order = orderRepository.findByOrderRefForUpdate(orderRef)
                .filter(o -> userId.equals(o.getUserId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Order can no longer be cancelled");
        }
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        restoreStock(items);
        couponService.release(order.getId());
        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(orderRepository.save(order), items);
    }

    /**
     * Customer asks to return a delivered order, within {@link Order#RETURN_WINDOW} of delivery. Moves it to
     * RETURN_REQUESTED for staff to process; stock is not restored here (staff inspect the goods first).
     */
    @Transactional
    public OrderResponse requestReturn(Long userId, String orderRef, String reason) {
        Order order = orderRepository.findByOrderRefForUpdate(orderRef)
                .filter(o -> userId.equals(o.getUserId()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only a delivered order can be returned");
        }
        Instant now = Instant.now();
        // Orders delivered before delivery times were recorded have no timestamp; fall back to last update.
        Instant deliveredAt = order.getDeliveredAt() != null ? order.getDeliveredAt() : order.getUpdatedAt();
        if (deliveredAt != null && now.isAfter(deliveredAt.plus(Order.RETURN_WINDOW))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "The " + Order.RETURN_WINDOW.toDays() + "-day return window for this order has ended");
        }
        order.setReturnReason(reason.trim());
        order.setReturnRequestedAt(now);
        order.setStatus(OrderStatus.RETURN_REQUESTED);
        return OrderResponse.from(orderRepository.save(order), orderItemRepository.findByOrderId(order.getId()));
    }

    /** A cancelled order gives back its coupon use. */
    public void releaseCoupon(Long orderId) {
        couponService.release(orderId);
    }

    /** Puts the quantities of a cancelled order back into variant stock. */
    public void restoreStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            if (item.getVariantId() == null) {
                continue;
            }
            variantRepository.findById(item.getVariantId()).ifPresent(v -> {
                v.setStockQuantity(v.getStockQuantity() + item.getQuantity());
                variantRepository.save(v);
            });
        }
    }

    private Order find(Long userId, String orderRef) {
        return orderRepository.findByOrderRefAndUserId(orderRef, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private String generateOrderRef() {
        String ref;
        do {
            ref = "MB" + LocalDate.now().format(REF_DATE) + String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (orderRepository.existsByOrderRef(ref));
        return ref;
    }
}
