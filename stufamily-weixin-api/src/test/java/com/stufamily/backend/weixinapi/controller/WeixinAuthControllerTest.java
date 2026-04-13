package com.stufamily.backend.weixinapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.identity.application.dto.LoginResult;
import com.stufamily.backend.identity.application.dto.WechatUserProfileView;
import com.stufamily.backend.identity.application.service.AuthApplicationService;
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

class WeixinAuthControllerTest {

    private MockMvc mockMvc;
    private AuthApplicationService authApplicationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authApplicationService = Mockito.mock(AuthApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new WeixinAuthController(authApplicationService)).build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginShouldReturnToken() throws Exception {
        when(authApplicationService.wechatLogin(any())).thenReturn(
            new LoginResult(1L, "jwt", "Bearer", "wx", List.of("WECHAT")));
        mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Req("code", "tom", "a"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("jwt"));
    }

    @Test
    void updateProfileShouldReturnProfile() throws Exception {
        mockUser();
        when(authApplicationService.updateWechatProfile(any()))
            .thenReturn(new WechatUserProfileView(9L, "测试用户", "13800138000", "https://example.com/a.png"));

        mockMvc.perform(put("/api/weixin/auth/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"测试用户\",\"phone\":\"13800138000\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.userId").value(9L))
            .andExpect(jsonPath("$.data.nickname").value("测试用户"))
            .andExpect(jsonPath("$.data.phone").value("13800138000"));
    }

    private void mockUser() {
        SecurityUser user = new SecurityUser(9L, "wx", "", List.of("WECHAT"));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private record Req(String code, String nickname, String avatarUrl) {
    }
}
