package com.mayilboutique.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mayilboutique.auth.dto.response.TokenResponse;
import com.mayilboutique.auth.entity.RefreshToken;
import com.mayilboutique.auth.repository.RefreshTokenRepository;
import com.mayilboutique.auth.repository.UserRepository;
import com.mayilboutique.core.config.JwtProperties;
import com.mayilboutique.core.entity.User;
import com.mayilboutique.core.enums.Role;
import com.mayilboutique.core.enums.UserStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.core.security.JwtTokenProvider;

class AuthServiceChangePasswordTest {

    private static final Long USER_ID = 1L;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        when(jwtProperties.getRefreshTokenValidityMs()).thenReturn(1_000L);
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("new-refresh");
        service = new AuthService(userRepository, refreshTokenRepository, encoder, jwtTokenProvider, jwtProperties,
                mock(OtpService.class));
    }

    private User user(Role role) {
        User u = new User("9360632795", encoder.encode("old-password"), role, UserStatus.ACTIVE);
        u.setId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void changesPasswordRevokesSessionsAndReturnsFreshTokens() {
        User u = user(Role.ADMIN);

        TokenResponse tokens = service.changeAdminPassword(USER_ID, "old-password", "brand-new-pass");

        assertEquals(true, encoder.matches("brand-new-pass", u.getPasswordHash()));
        verify(userRepository).save(u);
        verify(refreshTokenRepository).revokeAllForUser(USER_ID);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertEquals("new-access", tokens.getAccessToken());
        assertEquals("new-refresh", tokens.getRefreshToken());
    }

    @Test
    void wrongCurrentPasswordIsBadRequestNotUnauthorized() {
        User u = user(Role.ADMIN);
        String hashBefore = u.getPasswordHash();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeAdminPassword(USER_ID, "not-it", "brand-new-pass"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals(hashBefore, u.getPasswordHash());
        verify(refreshTokenRepository, never()).revokeAllForUser(any());
    }

    @Test
    void newPasswordMustDifferFromCurrent() {
        user(Role.ADMIN);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeAdminPassword(USER_ID, "old-password", "old-password"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void nonAdminAccountsAreRefused() {
        user(Role.CUSTOMER);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeAdminPassword(USER_ID, "old-password", "brand-new-pass"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }
}
