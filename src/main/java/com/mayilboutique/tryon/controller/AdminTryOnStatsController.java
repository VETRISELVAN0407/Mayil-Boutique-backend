package com.mayilboutique.tryon.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.tryon.dto.TryOnStatsResponse;
import com.mayilboutique.tryon.service.AdminTryOnStatsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/try-on/stats")
@RequiredArgsConstructor
public class AdminTryOnStatsController {

    private final AdminTryOnStatsService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TryOnStatsResponse> stats(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(service.stats(days));
    }
}
