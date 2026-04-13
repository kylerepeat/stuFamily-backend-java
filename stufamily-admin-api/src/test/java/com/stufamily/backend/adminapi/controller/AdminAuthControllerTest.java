package com.stufamily.backend.adminapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.identity.application.dto.LoginResult;
import com.stufamily.backend.identity.application.service.AuthApplicationService;
import com.stufamily.backend.shared.exception.GlobalExceptionHandler;
import com.stufamily.backend.shared.security.SecurityUser;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminAuthControllerTest {

    private MockMvc mockMvc;
    private AuthApplicationService authApplicationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authApplicationService = Mockito.mock(AuthApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminAuthController(authApplicationService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        SecurityUser principal = new SecurityUser(1L, "admin", "pwd", List.of("ADMIN"));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginShouldReturnToken() throws Exception {
        when(authApplicationService.adminLogin(any())).thenReturn(
            new LoginResult(1L, "jwt", "Bearer", "admin", List.of("ADMIN")));
        mockMvc.perform(post("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginReq("admin", "pwd"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("jwt"));
    }

    @Test
    void logoutShouldReturnOk() throws Exception {
        doNothing().when(authApplicationService).adminLogout(1L);
        mockMvc.perform(post("/api/admin/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));
    }

    private record LoginReq(String username, String password) {
    }
}
