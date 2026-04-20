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
public class WeixinFullFlowIntegrationTest {

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

    private static final Long TEST_USER_ID = 100L;

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
    @DisplayName("完整业务流程 - 新用户登录->完善资料->浏览商品->下单支付->添加家庭成员")
    void completeNewUserBusinessFlow() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"code\":\"full-flow-test-code\",\"nickname\":\"流程测试用户\",\"avatarUrl\":\"https://example.com/avatar.png\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.userId").exists())
            .andReturn().getResponse().getContentAsString();

        Long userId = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.data.userId");
        String accessToken = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.data.accessToken");

        mockMvc.perform(put("/api/weixin/auth/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"nickname\":\"更新后的昵称\",\"phone\":\"13900139999\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.userId").value(userId))
            .andExpect(jsonPath("$.data.nickname").value("更新后的昵称"))
            .andExpect(jsonPath("$.data.phone").value("13900139999"));

        mockMvc.perform(get("/api/weixin/home/index"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/weixin/home/products")
                .param("sale_start_at", "2026-01-01")
                .param("sale_end_at", "2026-12-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());

        String orderResponse = mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"productId\":1,\"quantity\":1,\"studentName\":\"小明学生\",\"guardianPhone\":\"13900139999\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderId").exists())
            .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
            .andReturn().getResponse().getContentAsString();

        Long orderId = com.jayway.jsonpath.JsonPath.read(orderResponse, "$.data.orderId");

        mockMvc.perform(get("/api/weixin/orders/" + orderId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderId").value(orderId));

        mockMvc.perform(post("/api/weixin/pay/create")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"orderId\":" + orderId + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.prepayId").exists())
            .andExpect(jsonPath("$.data.paySign").exists());

        mockMvc.perform(get("/api/weixin/orders/list")
                .header("Authorization", "Bearer " + accessToken)
                .param("page_no", "1")
                .param("page_size", "10")
                .param("status", "ALL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(post("/api/weixin/family/members")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"name\":\"家庭小宝\",\"relationship\":\"儿子\",\"birthDate\":\"2020-06-01\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("家庭小宝"));

        mockMvc.perform(get("/api/weixin/family/members")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/weixin/family/group/quota")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("多用户隔离流程 - 用户A订单用户B不可见")
    void multiUserDataIsolationFlow() throws Exception {
        String loginResponseA = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"code\":\"user-a-code\",\"nickname\":\"用户A\"}"))
            .andReturn().getResponse().getContentAsString();
        String tokenA = com.jayway.jsonpath.JsonPath.read(loginResponseA, "$.data.accessToken");
        Long userIdA = com.jayway.jsonpath.JsonPath.read(loginResponseA, "$.data.userId");

        String orderResponseA = mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"productId\":1,\"quantity\":1,\"studentName\":\"A的学生\",\"guardianPhone\":\"13800000001\"}"))
            .andReturn().getResponse().getContentAsString();
        Long orderIdA = com.jayway.jsonpath.JsonPath.read(orderResponseA, "$.data.orderId");

        String loginResponseB = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"code\":\"user-b-code\",\"nickname\":\"用户B\"}"))
            .andReturn().getResponse().getContentAsString();
        String tokenB = com.jayway.jsonpath.JsonPath.read(loginResponseB, "$.data.accessToken");
        Long userIdB = com.jayway.jsonpath.JsonPath.read(loginResponseB, "$.data.userId");

        mockMvc.perform(get("/api/weixin/orders/" + orderIdA)
                .header("Authorization", "Bearer " + tokenB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/weixin/family/members")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"name\":\"A的家人\",\"relationship\":\"女儿\",\"birthDate\":\"2021-01-01\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/weixin/family/members")
                .header("Authorization", "Bearer " + tokenB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("订单取消流程 - 创建订单后取消验证状态流转")
    void orderCancelFlowStateVerification() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"code\":\"cancel-flow-code\",\"nickname\":\"取消流程用户\"}"))
            .andReturn().getResponse().getContentAsString();
        String accessToken = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.data.accessToken");

        String orderResponse = mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"productId\":1,\"quantity\":1,\"studentName\":\"取消测试学生\",\"guardianPhone\":\"13800138000\"}"))
            .andReturn().getResponse().getContentAsString();
        Long orderId = com.jayway.jsonpath.JsonPath.read(orderResponse, "$.data.orderId");

        mockMvc.perform(get("/api/weixin/orders/" + orderId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));

        mockMvc.perform(post("/api/weixin/orders/" + orderId + "/cancel")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/weixin/orders/" + orderId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(post("/api/weixin/orders/" + orderId + "/cancel")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("权限边界测试 - 验证各模块未登录拦截")
    void permissionBoundaryTestAllModules() throws Exception {
        String[][] protectedEndpoints = {
            {"/api/weixin/orders/create", "POST"},
            {"/api/weixin/orders/list", "GET"},
            {"/api/weixin/orders/purchased-products", "GET"},
            {"/api/weixin/pay/create", "POST"},
            {"/api/weixin/family/members", "GET"},
            {"/api/weixin/family/members", "POST"},
            {"/api/weixin/family/group/quota", "GET"},
            {"/api/weixin/family/stats", "GET"},
            {"/api/weixin/home/messages", "POST"},
            {"/api/weixin/home/messages/mine", "GET"},
            {"/api/weixin/auth/profile", "PUT"}
        };

        for (String[] endpoint : protectedEndpoints) {
            String url = endpoint[0];
            String method = endpoint[1];

            if ("GET".equals(method)) {
                mockMvc.perform(get(url))
                    .andExpect(status().isUnauthorized());
            } else if ("POST".equals(method)) {
                mockMvc.perform(post(url).contentType(JSON_MEDIA_TYPE).content("{}"))
                    .andExpect(status().isUnauthorized());
            } else if ("PUT".equals(method)) {
                mockMvc.perform(put(url).contentType(JSON_MEDIA_TYPE).content("{}"))
                    .andExpect(status().isUnauthorized());
            }
        }

        mockMvc.perform(get("/api/weixin/home/index"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/weixin/home/products")
                .param("sale_start_at", "2026-01-01")
                .param("sale_end_at", "2026-12-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"code\":\"test-code\",\"nickname\":\"测试用户\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
