package com.mayilboutique.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.mayilboutique.core.config.JwtProperties;
import com.mayilboutique.core.entity.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = jwtProperties.getAccessTokenValidityMs() / 1000;
        this.refreshTokenValiditySeconds = jwtProperties.getRefreshTokenValidityMs() / 1000;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getMobile())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(new Date(java.util.Date.from(now).getTime()))
                .expiration(new Date(java.util.Date.from(now.plusSeconds(accessTokenValiditySeconds)).getTime()))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return java.util.UUID.randomUUID().toString();
    }

    public Claims validateAccessToken(String token) {
        return Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValidAccessToken(String token) {
        try {
            validateAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
