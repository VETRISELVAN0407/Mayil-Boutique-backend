package com.mayilboutique.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.auth.dto.request.ChangePasswordRequest;
import com.mayilboutique.auth.dto.response.TokenResponse;
import com.mayilboutique.auth.service.AuthService;
import com.mayilboutique.core.security.CustomUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/account")
public class AdminAccountController {

    private final AuthService authService;

    public AdminAccountController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TokenResponse> changePassword(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changeAdminPassword(
                principal.getUserId(), request.getCurrentPassword(), request.getNewPassword()));
    }
}
