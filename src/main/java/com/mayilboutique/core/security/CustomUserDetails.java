package com.mayilboutique.core.security;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.mayilboutique.core.enums.Role;
import com.mayilboutique.core.enums.UserStatus;

public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String mobile;
    private final String passwordHash;
    private final Role role;
    private final UserStatus status;

    public CustomUserDetails(Long userId, String mobile, String passwordHash, Role role, UserStatus status) {
        this.userId = userId;
        this.mobile = mobile;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public String getMobile() {
        return mobile;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public String getUsername() {
        return mobile;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    public Map<String, Object> getClaims() {
        return Map.of(
                "userId", String.valueOf(userId),
                "mobile", mobile,
                "role", role == null ? "CUSTOMER" : role.name());
    }
}
