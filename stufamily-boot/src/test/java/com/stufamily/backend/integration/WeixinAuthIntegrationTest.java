package com.stufamily.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.shared.security.JwtTokenProvider;
import com.stufamily.backend.wechat.gateway.WechatAuthGateway;
import com.stufamily.backend.wechat.gateway.WechatPayGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateResponse;
import com.stufamily.backend.wechat.gateway.dto.WechatSession;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.stufamily.backend.boot.StuFamilyBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WeixinAuthIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @MockBean
    protected WechatAuthGateway wechatAuthGateway;

    @MockBean
    protected WechatPayGateway wechatPayGateway;

    protected static final MediaType JSON_MEDIA_TYPE = MediaType.APPLICATION_JSON;

    @BeforeEach
    void setUpBase() {
        WechatSession mockSession = new WechatSession("test-openid-123", "test-unionid-123", "test-session-key");
        when(wechatAuthGateway.code2Session(any())).thenReturn(mockSession);

        WechatPayCreateResponse mockPayResponse = new WechatPayCreateResponse(
            "test-prepay-id-123",
            "test-nonce-str",
            "test-pay-sign",
            String.valueOf(System.currentTimeMillis() / 1000)
        );
        when(wechatPayGateway.createMiniappOrder(any())).thenReturn(mockPayResponse);
    }

    protected String createWechatUserToken(Long userId) {
        return jwtTokenProvider.createAccessToken(
            userId,
            "test-user-" + userId,
            List.of("WEIXIN"),
            com.stufamily.backend.shared.security.AuthAudience.WEIXIN
        );
    }

    protected String createAuthHeader(Long userId) {
        return "Bearer " + createWechatUserToken(userId);
    }

    @Test
    @DisplayName("微信登录成功 - 使用有效code登录返回token")
    void loginWithValidCodeShouldReturnToken() throws Exception {
        mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"code\":\"valid-test-code\",\"nickname\":\"测试用户\",\"avatarUrl\":\"https://example.com/avatar.png\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.userId").exists())
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("微信登录失败 - 空code返回参数错误")
    void loginWithEmptyCodeShouldReturnInvalidParam() throws Exception {
        mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"code\":\"\",\"nickname\":\"测试用户\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    @DisplayName("未登录访问受限接口 - 更新资料返回未授权")
    void accessProtectedEndpointWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(put("/api/weixin/auth/profile")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"nickname\":\"测试用户\",\"phone\":\"13800138000\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("未登录访问订单接口 - 返回未授权")
    void accessOrderEndpointWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/orders/purchased-products"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("未登录访问家庭接口 - 返回未授权")
    void accessFamilyEndpointWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/family/group/quota"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录后更新用户资料成功")
    void updateProfileAfterLoginShouldSucceed() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"code\":\"test-code-001\",\"nickname\":\"初始用户\"}"))
            .andReturn().getResponse().getContentAsString();

        com.jayway.jsonpath.JsonPath.compile("$.data.userId");
        Long userId = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.data.userId");
        String accessToken = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.data.accessToken");

        mockMvc.perform(put("/api/weixin/auth/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"nickname\":\"更新后昵称\",\"phone\":\"13800138999\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.userId").value(userId))
            .andExpect(jsonPath("$.data.nickname").value("更新后昵称"))
            .andExpect(jsonPath("$.data.phone").value("13800138999"));
    }

    @Test
    @DisplayName("更新资料参数错误 - 无效手机号格式")
    void updateProfileWithInvalidPhoneShouldReturnInvalidParam() throws Exception {
        mockMvc.perform(put("/api/weixin/auth/profile")
                .header("Authorization", createAuthHeader(1L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"nickname\":\"测试用户\",\"phone\":\"12345\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    @DisplayName("更新资料参数错误 - 昵称为空")
    void updateProfileWithEmptyNicknameShouldReturnInvalidParam() throws Exception {
        mockMvc.perform(put("/api/weixin/auth/profile")
                .header("Authorization", createAuthHeader(1L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"nickname\":\"\",\"phone\":\"13800138000\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    @DisplayName("使用无效token访问接口返回未授权")
    void accessWithInvalidTokenShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(put("/api/weixin/auth/profile")
                .header("Authorization", "Bearer invalid-token-12345")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"nickname\":\"测试用户\",\"phone\":\"13800138000\"}"))
            .andExpect(status().isUnauthorized());
    }
}
