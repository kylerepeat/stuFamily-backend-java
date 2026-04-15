package com.stufamily.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeixinAuthIntegrationTest extends IntegrationTestBase {

    @Test
    void login_shouldReturnToken_whenValidCode() throws Exception {
        mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "code", "test_code_123",
                    "nickname", "测试用户",
                    "avatarUrl", "https://example.com/avatar.png"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.roles[0]").value("WECHAT"));
    }

    @Test
    void login_shouldReturnToken_whenOnlyCodeProvided() throws Exception {
        mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "code", "test_code_456"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void updateProfile_shouldSuccess_whenValidInput() throws Exception {
        mockMvc.perform(put("/api/weixin/auth/profile")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "nickname", "新昵称",
                    "phone", "13900139000"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.nickname").value("新昵称"))
            .andExpect(jsonPath("$.data.phone").value("13900139000"));
    }

    @Test
    void updateProfile_shouldFail_whenInvalidPhone() throws Exception {
        mockMvc.perform(put("/api/weixin/auth/profile")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "nickname", "新昵称",
                    "phone", "invalid_phone"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void updateProfile_shouldFail_whenPhoneTooShort() throws Exception {
        mockMvc.perform(put("/api/weixin/auth/profile")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "nickname", "新昵称",
                    "phone", "1234567890"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void updateProfile_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(put("/api/weixin/auth/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "nickname", "新昵称",
                    "phone", "13900139000"
                ))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_shouldFail_whenInvalidToken() throws Exception {
        mockMvc.perform(put("/api/weixin/auth/profile")
                .header("Authorization", "Bearer invalid_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "nickname", "新昵称",
                    "phone", "13900139000"
                ))))
            .andExpect(status().isUnauthorized());
    }
}
