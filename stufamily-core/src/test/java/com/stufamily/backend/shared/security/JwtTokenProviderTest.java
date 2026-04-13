package com.stufamily.backend.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    @Test
    void shouldCreateAndParseToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("thisIsATestSecretKeyWithEnoughLength1234567890");
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        String token = provider.createAccessToken(10L, "admin", List.of("ADMIN"), AuthAudience.ADMIN);
        assertNotNull(token);

        var claims = provider.parseClaims(token);
        assertEquals("10", claims.getSubject());
        assertEquals("admin", claims.get("username"));
    }
}

