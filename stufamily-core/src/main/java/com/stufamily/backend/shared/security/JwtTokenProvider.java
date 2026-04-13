package com.stufamily.backend.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final JwtProperties properties;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    public String createAccessToken(Long userId, String username, List<String> roles, AuthAudience audience) {
        return createAccessToken(userId, username, roles, audience, 0L);
    }

    public String createAccessToken(Long userId, String username, List<String> roles, AuthAudience audience, long tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(properties.getIssuer())
            .subject(String.valueOf(userId))
            .claim("username", username)
            .claim("roles", roles)
            .claim("audience", audience.name())
            .claim("tv", tokenVersion)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(properties.getAccessExpireSeconds())))
            .signWith(secretKey())
            .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
