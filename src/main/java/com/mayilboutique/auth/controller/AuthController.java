package com.mayilboutique.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.auth.dto.request.AdminLoginRequest;
import com.mayilboutique.auth.dto.request.CustomerOtpRequest;
import com.mayilboutique.auth.dto.request.CustomerOtpVerifyRequest;
import com.mayilboutique.auth.dto.request.LogoutRequest;
import com.mayilboutique.auth.dto.request.RefreshTokenRequest;
import com.mayilboutique.auth.dto.response.AdminLoginResponse;
import com.mayilboutique.auth.dto.response.CustomerOtpResponse;
import com.mayilboutique.auth.dto.response.TokenResponse;
import com.mayilboutique.auth.service.AuthService;
import com.mayilboutique.auth.service.OtpService;
import com.mayilboutique.core.security.CustomUserDetails;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/admin/login")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/customer/otp/request")
    public ResponseEntity<CustomerOtpResponse> requestCustomerOtp(@Valid @RequestBody CustomerOtpRequest request) {
        return otpService.generateAndSendOtp(request.getMobile())
                .map(code -> ResponseEntity.ok(new CustomerOtpResponse(true, code,
                        "TEST MODE: real SMS/WhatsApp delivery is temporarily disabled. Use the OTP shown here.")))
                .orElseGet(() -> ResponseEntity.ok().build());
    }

    @PostMapping("/customer/otp/verify")
    public ResponseEntity<TokenResponse> verifyCustomerOtp(@Valid @RequestBody CustomerOtpVerifyRequest request) {
        TokenResponse tokenResponse = authService.loginByMobileOtp(request.getMobile(), request.getCode());
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/customer/refresh")
    public ResponseEntity<TokenResponse> refreshCustomer(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/customer/logout")
    public ResponseEntity<Void> logoutCustomer(@Valid @RequestBody LogoutRequest request) {
        authService.logoutByToken(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails principal,
                                      @Valid @RequestBody LogoutRequest request) {
        authService.logout(principal.getUserId(), request.getRefreshToken());
        return ResponseEntity.ok().build();
    }
}
