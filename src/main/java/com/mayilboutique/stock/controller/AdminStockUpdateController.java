package com.mayilboutique.stock.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.admin.service.AdminDashboardService;
import com.mayilboutique.core.dto.PageResponse;
import com.mayilboutique.stock.dto.AddStockRequest;
import com.mayilboutique.stock.dto.LowStockResponse;
import com.mayilboutique.stock.dto.StockUpdateResponse;
import com.mayilboutique.stock.service.StockUpdateService;

import jakarta.validation.Valid;

/**
 * Products, variants and their current stock are read with the existing
 * {@code GET /api/admin/products} and {@code GET /api/admin/products/{id}/variants}.
 */
@RestController
@RequestMapping("/api/admin/stock-updates")
public class AdminStockUpdateController {

    private final StockUpdateService stockUpdateService;

    public AdminStockUpdateController(StockUpdateService stockUpdateService) {
        this.stockUpdateService = stockUpdateService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockUpdateResponse> addStock(@Valid @RequestBody AddStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockUpdateService.addStock(request));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<LowStockResponse>> lowStock(
            @RequestParam(defaultValue = "" + AdminDashboardService.LOW_STOCK_THRESHOLD) int threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(stockUpdateService.lowStock(threshold, page, size));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<StockUpdateResponse>> history(
            @RequestParam(required = false) Long variantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(stockUpdateService.history(variantId, page, size));
    }
}
