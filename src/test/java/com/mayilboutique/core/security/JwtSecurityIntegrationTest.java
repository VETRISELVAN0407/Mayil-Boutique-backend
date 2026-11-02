package com.mayilboutique.core.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mayilboutique.auth.controller.AuthController;
import com.mayilboutique.auth.service.AuthService;
import com.mayilboutique.auth.service.OtpService;
import com.mayilboutique.core.config.SecurityConfig;
import com.mayilboutique.core.entity.User;
import com.mayilboutique.core.enums.Role;
import com.mayilboutique.core.enums.UserStatus;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Runs the real security filter chain (JWT filter + route rules) against real tokens. */
@WebMvcTest(controllers = {AuthController.class, JwtSecurityIntegrationTest.ProbeController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtSecurityIntegrationTest.ProbeController.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-0123456789-0123456789-0123456789",
        "jwt.access-token-validity-ms=900000",
        "jwt.refresh-token-validity-ms=2592000000"
})
class JwtSecurityIntegrationTest {

    private static final String SECRET = "test-secret-key-0123456789-0123456789-0123456789";
    private static final String OTP_BODY = "{\"mobile\":\"9876543210\"}";

    @RestController
    static class ProbeController {

        @GetMapping("/api/customer/probe")
        String customer(@AuthenticationPrincipal CustomUserDetails principal) {
            return principal.getUserId() + ":" + principal.getMobile();
        }

        @GetMapping("/api/admin/probe")
        String admin(@AuthenticationPrincipal CustomUserDetails principal) {
            return principal.getUserId() + ":" + principal.getRole();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokens;

    @MockBean
    private AuthService authService;

    @MockBean
    private OtpService otpService;

    private String tokenFor(Role role, long id) {
        User user = new User("9876543210", "x", role, UserStatus.ACTIVE);
        user.setId(id);
        return tokens.generateAccessToken(user);
    }

    private String expiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(3600);
        return Jwts.builder().subject("9876543210").claim("userId", 5L).claim("role", "CUSTOMER")
                .issuedAt(Date.from(past.minusSeconds(60))).expiration(Date.from(past)).signWith(key).compact();
    }

    // ---- valid tokens: the userId claim is a number and must be read as one

    @Test
    void validCustomerTokenAuthenticatesAndExposesNumericUserId() throws Exception {
        mockMvc.perform(get("/api/customer/probe").header("Authorization", "Bearer " + tokenFor(Role.CUSTOMER, 42L)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("42:9876543210"));
    }

    @Test
    void validAdminTokenReachesAdminRoute() throws Exception {
        mockMvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + tokenFor(Role.ADMIN, 1L)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("1:ADMIN"));
    }

    @Test
    void customerTokenIsForbiddenOnAdminRoute() throws Exception {
        mockMvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + tokenFor(Role.CUSTOMER, 42L)))
                .andExpect(status().isForbidden());
    }

    // ---- invalid tokens never throw out of the filter

    @Test
    void protectedRouteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/customer/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void protectedRouteWithMalformedTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/customer/probe").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteWithExpiredTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/customer/probe").header("Authorization", "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteWithTamperedSignatureIsUnauthorized() throws Exception {
        String token = tokenFor(Role.CUSTOMER, 42L);
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("aaa") ? "bbb" : "aaa");
        mockMvc.perform(get("/api/customer/probe").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    // ---- public OTP endpoints stay public whatever Authorization header the browser sends

    @Test
    void otpRequestIsPublicWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/customer/otp/request").contentType(MediaType.APPLICATION_JSON).content(OTP_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void otpRequestWorksWhenBrowserSendsAValidStoredToken() throws Exception {
        mockMvc.perform(post("/api/auth/customer/otp/request")
                        .header("Authorization", "Bearer " + tokenFor(Role.CUSTOMER, 42L))
                        .contentType(MediaType.APPLICATION_JSON).content(OTP_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void otpRequestWorksWhenBrowserSendsAnExpiredOrGarbageToken() throws Exception {
        mockMvc.perform(post("/api/auth/customer/otp/request")
                        .header("Authorization", "Bearer " + expiredToken())
                        .contentType(MediaType.APPLICATION_JSON).content(OTP_BODY))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/customer/otp/request")
                        .header("Authorization", "Bearer garbage")
                        .contentType(MediaType.APPLICATION_JSON).content(OTP_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void otpVerifyIsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/customer/otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"9876543210\",\"code\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void otpRequestStillValidatesTheMobileNumber() throws Exception {
        mockMvc.perform(post("/api/auth/customer/otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }
}
