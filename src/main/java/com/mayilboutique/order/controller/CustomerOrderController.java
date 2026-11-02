package com.mayilboutique.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.core.security.CustomUserDetails;
import com.mayilboutique.order.dto.request.PlaceOrderRequest;
import com.mayilboutique.order.dto.request.ReturnOrderRequest;
import com.mayilboutique.order.dto.response.OrderResponse;
import com.mayilboutique.order.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customer/orders")
public class CustomerOrderController {

    private final OrderService orderService;

    public CustomerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal CustomUserDetails principal,
                                               @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(principal.getUserId(), request.getAddressId(), request.getCouponCode()));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(orderService.list(principal.getUserId()));
    }

    @GetMapping("/{orderRef}")
    public ResponseEntity<OrderResponse> get(@AuthenticationPrincipal CustomUserDetails principal,
                                             @PathVariable String orderRef) {
        return ResponseEntity.ok(orderService.get(principal.getUserId(), orderRef));
    }

    @PostMapping("/{orderRef}/cancel")
    public ResponseEntity<OrderResponse> cancel(@AuthenticationPrincipal CustomUserDetails principal,
                                                @PathVariable String orderRef) {
        return ResponseEntity.ok(orderService.cancel(principal.getUserId(), orderRef));
    }

    @PostMapping("/{orderRef}/return")
    public ResponseEntity<OrderResponse> requestReturn(@AuthenticationPrincipal CustomUserDetails principal,
                                                       @PathVariable String orderRef,
                                                       @Valid @RequestBody ReturnOrderRequest request) {
        return ResponseEntity.ok(orderService.requestReturn(principal.getUserId(), orderRef, request.getReason()));
    }
}
