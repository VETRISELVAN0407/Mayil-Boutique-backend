package com.mayilboutique.tryon.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.tryon.dto.TryOnProductResponse;
import com.mayilboutique.tryon.provider.TryOnCapabilities;
import com.mayilboutique.tryon.service.TryOnConfigService;

import lombok.RequiredArgsConstructor;

/** Public (under {@code /api/products/**}): catalogue data only, no customer data. Jobs come in a later phase. */
@RestController
@RequestMapping("/api/products/try-on")
@RequiredArgsConstructor
public class PublicTryOnController {

    private final TryOnConfigService service;
    private final TryOnCapabilities capabilities;

    public record Status(boolean available) {
    }

    /** Lets the storefront hide Virtual Try-On entirely until it really works. */
    @GetMapping("/status")
    public ResponseEntity<Status> status() {
        return ResponseEntity.ok(new Status(capabilities.available()));
    }

    @GetMapping
    public ResponseEntity<List<TryOnProductResponse>> list(@RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(service.listEligible(categoryId));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<TryOnProductResponse> get(@PathVariable Long productId) {
        return ResponseEntity.ok(service.getEligible(productId));
    }
}
