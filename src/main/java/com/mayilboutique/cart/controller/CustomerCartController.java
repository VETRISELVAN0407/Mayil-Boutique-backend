package com.mayilboutique.cart.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.cart.dto.request.AddToCartRequest;
import com.mayilboutique.cart.dto.request.MergeCartRequest;
import com.mayilboutique.cart.dto.request.UpdateCartItemRequest;
import com.mayilboutique.cart.dto.response.CartItemResponse;
import com.mayilboutique.cart.service.CartService;
import com.mayilboutique.core.security.CustomUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customer/cart")
public class CustomerCartController {

    private final CartService cartService;

    public CustomerCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<List<CartItemResponse>> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(cartService.list(principal.getUserId()));
    }

    @PostMapping
    public ResponseEntity<CartItemResponse> add(@AuthenticationPrincipal CustomUserDetails principal,
                                                @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.add(principal.getUserId(), request.getVariantId(), request.getQuantity()));
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<CartItemResponse> update(@AuthenticationPrincipal CustomUserDetails principal,
                                                   @PathVariable Long variantId,
                                                   @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.update(principal.getUserId(), variantId, request.getQuantity()));
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal CustomUserDetails principal,
                                       @PathVariable Long variantId) {
        cartService.remove(principal.getUserId(), variantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge")
    public ResponseEntity<List<CartItemResponse>> merge(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @Valid @RequestBody MergeCartRequest request) {
        return ResponseEntity.ok(cartService.merge(principal.getUserId(), request.getItems()));
    }
}
