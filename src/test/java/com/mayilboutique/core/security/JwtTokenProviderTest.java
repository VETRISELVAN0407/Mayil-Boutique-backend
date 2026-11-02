package com.mayilboutique.core.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mayilboutique.core.config.JwtProperties;
import com.mayilboutique.core.entity.User;
import com.mayilboutique.core.enums.Role;
import com.mayilboutique.core.enums.UserStatus;

import io.jsonwebtoken.Claims;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-0123456789-0123456789-0123456789");
        jwtProperties.setAccessTokenValidityMs(900000L);
        jwtProperties.setRefreshTokenValidityMs(2592000000L);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    void generatesAndValidatesAccessToken() {
        User user = new User("9999999999", "hashed-password", Role.ADMIN, UserStatus.ACTIVE);
        user.setId(1L);

        String token = jwtTokenProvider.generateAccessToken(user);

        assertTrue(jwtTokenProvider.isValidAccessToken(token));
        Claims claims = jwtTokenProvider.validateAccessToken(token);
        assertEquals("9999999999", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void rejectsInvalidAccessToken() {
        assertFalse(jwtTokenProvider.isValidAccessToken("invalid-token-value"));
    }
}
