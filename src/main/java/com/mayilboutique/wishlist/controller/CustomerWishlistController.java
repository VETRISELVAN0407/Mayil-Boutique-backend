package com.mayilboutique.wishlist.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.core.security.CustomUserDetails;
import com.mayilboutique.wishlist.dto.request.AddToWishlistRequest;
import com.mayilboutique.wishlist.dto.request.MergeWishlistRequest;
import com.mayilboutique.wishlist.dto.response.WishlistItemResponse;
import com.mayilboutique.wishlist.service.WishlistService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customer/wishlist")
public class CustomerWishlistController {

    private final WishlistService wishlistService;

    public CustomerWishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(wishlistService.list(principal.getUserId()));
    }

    @PostMapping
    public ResponseEntity<WishlistItemResponse> add(@AuthenticationPrincipal CustomUserDetails principal,
                                                    @Valid @RequestBody AddToWishlistRequest request) {
        return ResponseEntity.ok(wishlistService.add(principal.getUserId(), request.getProductId()));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal CustomUserDetails principal,
                                       @PathVariable Long productId) {
        wishlistService.remove(principal.getUserId(), productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge")
    public ResponseEntity<List<WishlistItemResponse>> merge(@AuthenticationPrincipal CustomUserDetails principal,
                                                            @Valid @RequestBody MergeWishlistRequest request) {
        return ResponseEntity.ok(wishlistService.merge(principal.getUserId(), request.getProductIds()));
    }
}
