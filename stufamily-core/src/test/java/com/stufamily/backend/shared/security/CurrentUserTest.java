package com.stufamily.backend.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireUserIdShouldReturnPrincipalId() {
        SecurityUser user = new SecurityUser(88L, "u", "", List.of("WECHAT"));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        assertEquals(88L, CurrentUser.requireUserId());
    }
}

