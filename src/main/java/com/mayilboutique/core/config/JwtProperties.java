package com.mayilboutique.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenValidityMs = 900000L;
    private long refreshTokenValidityMs = 2592000000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenValidityMs() {
        return accessTokenValidityMs;
    }

    public void setAccessTokenValidityMs(long accessTokenValidityMs) {
        this.accessTokenValidityMs = accessTokenValidityMs;
    }

    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }

    public void setRefreshTokenValidityMs(long refreshTokenValidityMs) {
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }
}
