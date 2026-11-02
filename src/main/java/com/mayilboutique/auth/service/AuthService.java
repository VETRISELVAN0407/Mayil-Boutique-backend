package com.mayilboutique.auth.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mayilboutique.auth.dto.request.AdminLoginRequest;
import com.mayilboutique.auth.dto.response.AdminLoginResponse;
import com.mayilboutique.auth.dto.response.TokenResponse;
import com.mayilboutique.auth.entity.RefreshToken;
import com.mayilboutique.auth.repository.RefreshTokenRepository;
import com.mayilboutique.auth.repository.UserRepository;
import com.mayilboutique.auth.service.OtpService;
import com.mayilboutique.core.config.JwtProperties;
import com.mayilboutique.core.entity.User;
import com.mayilboutique.core.enums.Role;
import com.mayilboutique.core.enums.UserStatus;
import com.mayilboutique.core.exception.BusinessException;
import com.mayilboutique.core.security.JwtTokenProvider;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       JwtProperties jwtProperties,
                       OtpService otpService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.otpService = otpService;
    }

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        User user = userRepository.findByMobile(request.getMobile())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid mobile number or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid mobile number or password");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }
        if (user.getRole() != Role.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Admin access required");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        saveRefreshToken(user, refreshToken);

        return new AdminLoginResponse(accessToken, refreshToken, user.getId(), user.getMobile(), user.getRole().name());
    }

    /**
     * Changes an admin's password, ends all their sessions and returns a fresh token pair so the caller stays
     * signed in. A wrong current password is a 400 on purpose: a 401 would make the client treat the session as expired.
     */
    @Transactional
    public TokenResponse changeAdminPassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Account not found"));
        if (user.getRole() != Role.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "New password must be different from the current one");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(userId);
        return issueCustomerTokens(user);
    }

    // A wrong OTP must still persist its failed-attempt count, so business errors do not roll back.
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse loginByMobileOtp(String mobile, String code) {
        User user = otpService.verifyOtp(mobile, code);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }
        if (user.getRole() != Role.CUSTOMER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Customer access required");
        }
        return issueCustomerTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (token.isRevoked() || token.getExpiresAt().isBefore(java.time.Clock.systemUTC().instant())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }

        User user = token.getUser();
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        saveRefreshToken(user, newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(Long userId, String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken).orElse(null);
        if (token != null && token.getUser().getId().equals(userId)) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }
    }

    /** Revokes a refresh token without needing a valid access token (customer sessions may have an expired one). */
    @Transactional
    public void logoutByToken(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private TokenResponse issueCustomerTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        saveRefreshToken(user, refreshToken);
        return new TokenResponse(accessToken, refreshToken);
    }

    private void saveRefreshToken(User user, String refreshToken) {
        Instant expiry = Instant.now().plus(Duration.ofMillis(jwtProperties.getRefreshTokenValidityMs()));
        refreshTokenRepository.save(new RefreshToken(user, refreshToken, expiry));
    }
}
