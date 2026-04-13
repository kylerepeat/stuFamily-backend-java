package com.stufamily.backend.adminapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.identity.application.dto.AdminAccountView;
import com.stufamily.backend.identity.application.service.AdminAccountApplicationService;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.exception.GlobalExceptionHandler;
import com.stufamily.backend.shared.security.SecurityUser;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminAccountControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private AdminAccountApplicationService adminAccountApplicationService;

    @BeforeEach
    void setUp() {
        adminAccountApplicationService = Mockito.mock(AdminAccountApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminAccountController(adminAccountApplicationService))
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
    void shouldListAccounts() throws Exception {
        when(adminAccountApplicationService.listAdminAccounts("a", "ACTIVE", 1, 20))
            .thenReturn(PageResult.of(List.of(new AdminAccountView(1L, "U1", "admin", "ADMIN", "ACTIVE",
                "nick", "138", "a@b.com", null, null)), 1, 1, 20));

        mockMvc.perform(get("/api/admin/accounts")
                .param("keyword", "a")
                .param("status", "ACTIVE")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].username").value("admin"))
            .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldCreateAccount() throws Exception {
        when(adminAccountApplicationService.createAdminAccount(any()))
            .thenReturn(new AdminAccountView(9L, "U9", "new_admin", "ADMIN", "ACTIVE",
                "nick", "138", "new@b.com", null, null));

        mockMvc.perform(post("/api/admin/accounts")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new CreateReq("new_admin", "Aa1@5678",
                    "nick", "138", "new@b.com"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(9));
    }

    @Test
    void shouldDisableAccount() throws Exception {
        doNothing().when(adminAccountApplicationService).disableAdminAccount(9L, 1L);
        mockMvc.perform(post("/api/admin/accounts/9/disable"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void shouldChangePassword() throws Exception {
        doNothing().when(adminAccountApplicationService).changeAdminPassword(any());
        mockMvc.perform(post("/api/admin/accounts/9/password")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new PasswordReq("Aa1@new88"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void shouldValidatePasswordStrength() throws Exception {
        doNothing().when(adminAccountApplicationService).validatePasswordStrength("Aa1@new88", "admin");
        mockMvc.perform(post("/api/admin/accounts/password/validate")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new ValidateReq("Aa1@new88", "admin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));
    }

    private record CreateReq(String username, String password, String nickname, String phone, String email) {
    }

    private record PasswordReq(String newPassword) {
    }

    private record ValidateReq(String password, String username) {
    }
}
