package com.mayilboutique.tryon.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.tryon.dto.TryOnConfigResponse;
import com.mayilboutique.tryon.dto.UpdateTryOnConfigRequest;
import com.mayilboutique.tryon.service.TryOnConfigService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/products/{productId}/try-on")
@RequiredArgsConstructor
public class AdminTryOnConfigController {

    private final TryOnConfigService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TryOnConfigResponse> get(@PathVariable Long productId) {
        return ResponseEntity.ok(service.get(productId));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TryOnConfigResponse> update(@PathVariable Long productId,
                                                      @Valid @RequestBody UpdateTryOnConfigRequest request) {
        return ResponseEntity.ok(service.update(productId, request));
    }
}
