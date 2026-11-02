package com.mayilboutique.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mayilboutique.core.enums.Role;
import com.mayilboutique.core.enums.UserStatus;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null) {
            try {
                authenticate(token, request);
            } catch (JwtException | IllegalArgumentException e) {
                // Missing/expired/forged/malformed token: continue unauthenticated. Public endpoints still work
                // and protected ones are rejected with 401 by the entry point instead of failing inside the filter.
                SecurityContextHolder.clearContext();
                log.debug("Ignoring invalid access token: {}", e.getClass().getSimpleName());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        Claims claims = jwtTokenProvider.validateAccessToken(token);
        CustomUserDetails principal = new CustomUserDetails(
                readUserId(claims),
                claims.getSubject(),
                null,
                Role.valueOf(claims.get("role", String.class)),
                UserStatus.ACTIVE);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(request);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * The token generator writes {@code userId} as a number, which JJWT hands back as an Integer (or Long
     * for large ids), never a String. Numeric strings are tolerated too.
     */
    private static Long readUserId(Claims claims) {
        Object value = claims.get("userId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        throw new IllegalArgumentException("Token has no usable userId claim");
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
