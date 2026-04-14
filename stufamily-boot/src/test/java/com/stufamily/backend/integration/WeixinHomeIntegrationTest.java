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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.stufamily.backend.boot.StuFamilyBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WeixinHomeIntegrationTest {

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
    @DisplayName("访问首页接口 - 无需登录返回首页数据")
    void getHomeIndexWithoutLoginShouldReturnData() throws Exception {
        mockMvc.perform(get("/api/weixin/home/index"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("获取商品列表 - 带正确日期参数返回商品数据")
    void getProductsWithValidDateShouldReturnList() throws Exception {
        mockMvc.perform(get("/api/weixin/home/products")
                .param("sale_start_at", "2026-01-01")
                .param("sale_end_at", "2026-12-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("获取商品列表 - 缺失日期参数返回参数错误")
    void getProductsWithoutDateParamsShouldReturnInvalidParam() throws Exception {
        mockMvc.perform(get("/api/weixin/home/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    @DisplayName("获取商品列表 - 日期格式错误返回参数错误")
    void getProductsWithInvalidDateFormatShouldReturnInvalidParam() throws Exception {
        mockMvc.perform(get("/api/weixin/home/products")
                .param("sale_start_at", "invalid-date")
                .param("sale_end_at", "2026-12-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    @DisplayName("获取商品详情 - 无效商品ID返回业务错误")
    void getProductDetailWithInvalidIdShouldReturnError() throws Exception {
        mockMvc.perform(get("/api/weixin/home/products/999999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("未登录提交家长留言 - 返回未授权")
    void createMessageWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/weixin/home/messages")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"content\":\"这是一条测试留言\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录后提交家长留言 - 成功")
    void createMessageAfterLoginShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/weixin/home/messages")
                .header("Authorization", createAuthHeader(1L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"content\":\"这是一条测试留言内容，最长支持500字符\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.content").value("这是一条测试留言内容，最长支持500字符"));
    }

    @Test
    @DisplayName("提交家长留言 - 空内容返回参数错误")
    void createMessageWithEmptyContentShouldReturnInvalidParam() throws Exception {
        mockMvc.perform(post("/api/weixin/home/messages")
                .header("Authorization", createAuthHeader(1L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"content\":\"\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    @DisplayName("登录后查询我的留言列表 - 成功")
    void getMyMessagesAfterLoginShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/weixin/home/messages")
                .header("Authorization", createAuthHeader(2L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"content\":\"第一条留言\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/weixin/home/messages/mine")
                .header("Authorization", createAuthHeader(2L))
                .param("page_no", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.total").exists());
    }

    @Test
    @DisplayName("未登录查询我的留言列表 - 返回未授权")
    void getMyMessagesWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/home/messages/mine"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("留言列表分页参数 - 超出最大页大小自动限制")
    void getMyMessagesWithExceedPageSizeShouldBeLimited() throws Exception {
        mockMvc.perform(get("/api/weixin/home/messages/mine")
                .header("Authorization", createAuthHeader(1L))
                .param("page_size", "1000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
