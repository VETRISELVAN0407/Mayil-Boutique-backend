package com.mayilboutique.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.mayilboutique.auth.dto.request.AdminLoginRequest;
import com.mayilboutique.auth.dto.response.AdminLoginResponse;
import com.mayilboutique.auth.dto.response.TokenResponse;
import com.mayilboutique.auth.service.AuthService;
import com.mayilboutique.auth.service.OtpService;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.core.exception.GlobalExceptionHandler;

class AuthControllerTest {

    private AuthService authService;
    private OtpService otpService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = org.mockito.Mockito.mock(AuthService.class);
        otpService = org.mockito.Mockito.mock(OtpService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, otpService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginReturnsAccessTokenAndRefreshToken() throws Exception {
        AdminLoginResponse response = new AdminLoginResponse("access-token", "refresh-token", 1L, "9999999999", "ADMIN");
        given(authService.login(any(AdminLoginRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"9999999999\",\"password\":\"Admin@12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.mobile").value("9999999999"));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        given(authService.login(any(AdminLoginRequest.class)))
                .willThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid mobile number or password"));

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"9999999999\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid mobile number or password"));
    }

    @Test
    void loginReturnsBadRequestWhenMobileIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Admin@12345\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestCustomerOtpReturnsOk() throws Exception {
        mockMvc.perform(post("/api/auth/customer/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"9999999999\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void requestCustomerOtpInTestModeReturnsTheCode() throws Exception {
        given(otpService.generateAndSendOtp("9999999999")).willReturn(java.util.Optional.of("123456"));

        mockMvc.perform(post("/api/auth/customer/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"9999999999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testMode").value(true))
                .andExpect(jsonPath("$.testOtp").value("123456"));
    }

    @Test
    void requestCustomerOtpWithRealDeliveryHasNoBody() throws Exception {
        mockMvc.perform(post("/api/auth/customer/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"9999999999\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));
    }

    @Test
    void verifyCustomerOtpReturnsTokenResponse() throws Exception {
        TokenResponse response = new TokenResponse("customer-access-token", "customer-refresh-token");
        given(authService.loginByMobileOtp("9999999999", "123456")).willReturn(response);

        mockMvc.perform(post("/api/auth/customer/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"9999999999\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("customer-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("customer-refresh-token"));
    }

    @Test
    void verifyCustomerOtpReturnsBadRequestWhenMobileIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/customer/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isBadRequest());
    }
}
