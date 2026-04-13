package com.stufamily.backend.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SecurityUserTest {

    @Test
    void shouldMapRoleToGrantedAuthority() {
        SecurityUser user = new SecurityUser(1L, "admin", "pwd", List.of("ADMIN"));
        assertEquals("ROLE_ADMIN", user.getAuthorities().iterator().next().getAuthority());
    }
}

