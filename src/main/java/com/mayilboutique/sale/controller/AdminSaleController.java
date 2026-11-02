package com.mayilboutique.sale.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.sale.dto.SaleRequest;
import com.mayilboutique.sale.dto.SaleResponse;
import com.mayilboutique.sale.service.AdminSaleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/sales")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSaleController {

    private final AdminSaleService service;

    public AdminSaleController(AdminSaleService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<SaleResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SaleResponse> update(@PathVariable Long id, @Valid @RequestBody SaleRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    /** Body: {@code {"active": true|false}}. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<SaleResponse> setStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        if (active == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "active is required");
        }
        return ResponseEntity.ok(service.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
