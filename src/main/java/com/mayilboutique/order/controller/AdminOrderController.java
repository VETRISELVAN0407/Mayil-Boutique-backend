package com.mayilboutique.order.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.core.dto.PageResponse;
import com.mayilboutique.core.enums.OrderStatus;
import com.mayilboutique.order.dto.request.UpdateOrderNoteRequest;
import com.mayilboutique.order.dto.request.UpdateOrderStatusRequest;
import com.mayilboutique.order.dto.response.OrderResponse;
import com.mayilboutique.order.service.AdminOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderResponse>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminOrderService.search(status, q, from, to, page, size));
    }

    @GetMapping("/{orderRef}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> get(@PathVariable String orderRef) {
        return ResponseEntity.ok(adminOrderService.get(orderRef));
    }

    @PatchMapping("/{orderRef}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable String orderRef,
                                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(adminOrderService.updateStatus(orderRef, request.getStatus()));
    }

    @PutMapping("/{orderRef}/note")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateNote(@PathVariable String orderRef,
                                                    @Valid @RequestBody UpdateOrderNoteRequest request) {
        return ResponseEntity.ok(adminOrderService.updateNote(orderRef, request.getNote()));
    }

    @PatchMapping("/{orderRef}/payment/paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> markPaid(@PathVariable String orderRef) {
        return ResponseEntity.ok(adminOrderService.markPaid(orderRef));
    }
}
