package com.stufamily.backend.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {
    private String issuer = "stuFamily";
    private String secret = "stuFamilySecretKeyForJwtMustBeLongEnough123456";
    private long accessExpireSeconds = 7200;
    private long refreshExpireSeconds = 604800;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }

    public void setAccessExpireSeconds(long accessExpireSeconds) {
        this.accessExpireSeconds = accessExpireSeconds;
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireSeconds;
    }

    public void setRefreshExpireSeconds(long refreshExpireSeconds) {
        this.refreshExpireSeconds = refreshExpireSeconds;
    }
}

