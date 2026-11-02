package com.mayilboutique.tryon.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.tryon.GarmentType;
import com.mayilboutique.tryon.provider.TryOnCapabilities;

import lombok.RequiredArgsConstructor;

/** Tells the admin screens which garment types the connected engine really supports. */
@RestController
@RequestMapping("/api/admin/try-on/capabilities")
@RequiredArgsConstructor
public class AdminTryOnCapabilitiesController {

    public record Capabilities(boolean providerConfigured, List<GarmentType> supportedGarmentTypes) {
    }

    private final TryOnCapabilities capabilities;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Capabilities> get() {
        return ResponseEntity.ok(new Capabilities(capabilities.providerConfigured(), capabilities.supportedTypes()));
    }
}
