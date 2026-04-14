package com.stufamily.backend.integration;

import com.stufamily.backend.wechat.gateway.WechatAuthGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("微信认证接口集成测试")
public class WeixinAuthIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private WechatAuthGateway wechatAuthGateway;

    private static final String LOGIN_URL = "/api/weixin/auth/login";
    private static final String PROFILE_URL = "/api/weixin/auth/profile";

    @Nested
    @DisplayName("微信登录接口")
    class LoginTests {

        @Test
        @DisplayName("登录成功 - 新用户首次登录")
        void login_newUser_success() throws Exception {
            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession("openid_new_user_001", null, "session_key_001"));

            String requestBody = objectMapper.writeValueAsString(new LoginRequest(
                "test_code_001",
                "测试用户",
                "https://example.com/avatar.png"
            ));

            MvcResult result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.roles", hasItem("WECHAT")))
                .andReturn();

            Long userId = extractDataFieldAsLong(result, "userId");
            assertNotNull(userId);
            assertTrue(userId > 0);
        }

        @Test
        @DisplayName("登录成功 - 已有用户登录")
        void login_existingUser_success() throws Exception {
            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession("openid_postman_admin_001", null, "session_key_existing"));

            String requestBody = objectMapper.writeValueAsString(new LoginRequest(
                "test_code_existing",
                "更新昵称",
                "https://example.com/new-avatar.png"
            ));

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.roles", hasItem("WECHAT")));
        }

        @Test
        @DisplayName("登录失败 - 微信code无效")
        void login_invalidCode_failure() throws Exception {
            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession(null, null, null));

            String requestBody = objectMapper.writeValueAsString(new LoginRequest(
                "invalid_code",
                null,
                null
            ));

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
        }

        @Test
        @DisplayName("登录失败 - 缺少code参数")
        void login_missingCode_failure() throws Exception {
            String requestBody = "{}";

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("登录失败 - code为空字符串")
        void login_emptyCode_failure() throws Exception {
            String requestBody = objectMapper.writeValueAsString(new LoginRequest("", null, null));

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("更新用户资料接口")
    class UpdateProfileTests {

        @Test
        @DisplayName("更新成功 - 已登录用户更新昵称和手机号")
        void updateProfile_success() throws Exception {
            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession("openid_profile_test_001", null, "session_key"));

            String loginBody = objectMapper.writeValueAsString(new LoginRequest("code_profile", "用户", null));
            MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

            String token = extractDataField(loginResult, "accessToken");

            String updateBody = objectMapper.writeValueAsString(new UpdateProfileRequest(
                "新昵称",
                "13800138000"
            ));

            mockMvc.perform(withAuth(put(PROFILE_URL), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("新昵称"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"));
        }

        @Test
        @DisplayName("更新失败 - 未登录")
        void updateProfile_notLoggedIn_failure() throws Exception {
            String updateBody = objectMapper.writeValueAsString(new UpdateProfileRequest(
                "新昵称",
                "13800138000"
            ));

            mockMvc.perform(put(PROFILE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateBody))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("更新失败 - 手机号格式错误")
        void updateProfile_invalidPhone_failure() throws Exception {
            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession("openid_invalid_phone_001", null, "session_key"));

            String loginBody = objectMapper.writeValueAsString(new LoginRequest("code_invalid_phone", "用户", null));
            MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

            String token = extractDataField(loginResult, "accessToken");

            String updateBody = objectMapper.writeValueAsString(new UpdateProfileRequest(
                "新昵称",
                "invalid_phone"
            ));

            mockMvc.perform(withAuth(put(PROFILE_URL), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("更新失败 - 昵称为空")
        void updateProfile_emptyNickname_failure() throws Exception {
            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession("openid_empty_nick_001", null, "session_key"));

            String loginBody = objectMapper.writeValueAsString(new LoginRequest("code_empty_nick", "用户", null));
            MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

            String token = extractDataField(loginResult, "accessToken");

            String updateBody = objectMapper.writeValueAsString(new UpdateProfileRequest(
                "",
                "13800138000"
            ));

            mockMvc.perform(withAuth(put(PROFILE_URL), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("更新失败 - 手机号为空")
        void updateProfile_emptyPhone_failure() throws Exception {
            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession("openid_empty_phone_001", null, "session_key"));

            String loginBody = objectMapper.writeValueAsString(new LoginRequest("code_empty_phone", "用户", null));
            MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

            String token = extractDataField(loginResult, "accessToken");

            String updateBody = objectMapper.writeValueAsString(new UpdateProfileRequest(
                "新昵称",
                ""
            ));

            mockMvc.perform(withAuth(put(PROFILE_URL), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateBody))
                .andExpect(status().isBadRequest());
        }
    }

    private record LoginRequest(String code, String nickname, String avatarUrl) {}

    private record UpdateProfileRequest(String nickname, String phone) {}
}
