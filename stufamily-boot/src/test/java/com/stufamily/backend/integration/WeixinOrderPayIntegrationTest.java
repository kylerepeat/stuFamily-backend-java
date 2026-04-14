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
public class WeixinOrderPayIntegrationTest {

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

    private String createFamilyCardOrderJson() {
        return "{\"productId\":1,\"quantity\":1,\"studentName\":\"测试学生\",\"guardianPhone\":\"13800138000\",\"remark\":\"测试备注\"}";
    }

    private String createPaymentJson(Long orderId) {
        return "{\"orderId\":" + orderId + "}";
    }

    @Test
    @DisplayName("未登录创建订单 - 返回未授权")
    void createOrderWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/create")
                .contentType(JSON_MEDIA_TYPE)
                .content(createFamilyCardOrderJson()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录后创建家庭卡订单 - 成功创建返回订单信息")
    void createFamilyCardOrderAfterLoginShouldSucceed() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"code\":\"test-order-user-001\",\"nickname\":\"下单用户\"}"))
            .andReturn().getResponse().getContentAsString();

        String accessToken = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.data.accessToken");

        mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(JSON_MEDIA_TYPE)
                .content(createFamilyCardOrderJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.orderId").exists())
            .andExpect(jsonPath("$.data.orderNo").exists())
            .andExpect(jsonPath("$.data.totalAmount").exists())
            .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
    }

    @Test
    @DisplayName("创建订单 - 商品ID无效返回业务错误")
    void createOrderWithInvalidProductIdShouldReturnError() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", createAuthHeader(1L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"productId\":9999,\"quantity\":1,\"studentName\":\"测试学生\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("创建订单 - 数量小于1返回参数错误")
    void createOrderWithInvalidQuantityShouldReturnInvalidParam() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", createAuthHeader(1L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"productId\":1,\"quantity\":0,\"studentName\":\"测试学生\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    @DisplayName("创建订单 - 学生姓名为空返回参数错误")
    void createOrderWithEmptyStudentNameShouldReturnInvalidParam() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", createAuthHeader(1L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"productId\":1,\"quantity\":1,\"studentName\":\"\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    @DisplayName("未登录发起支付 - 返回未授权")
    void createPaymentWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/weixin/pay/create")
                .contentType(JSON_MEDIA_TYPE)
                .content(createPaymentJson(1L)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录后创建订单并发起支付 - 获得微信支付参数")
    void createOrderAndPayAfterLoginShouldReturnPayParams() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"code\":\"test-pay-user-001\",\"nickname\":\"支付用户\"}"))
            .andReturn().getResponse().getContentAsString();

        String accessToken = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.data.accessToken");

        String orderResponse = mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(JSON_MEDIA_TYPE)
                .content(createFamilyCardOrderJson()))
            .andReturn().getResponse().getContentAsString();

        Long orderId = com.jayway.jsonpath.JsonPath.read(orderResponse, "$.data.orderId");

        mockMvc.perform(post("/api/weixin/pay/create")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(JSON_MEDIA_TYPE)
                .content(createPaymentJson(orderId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.prepayId").exists())
            .andExpect(jsonPath("$.data.nonceStr").exists())
            .andExpect(jsonPath("$.data.paySign").exists())
            .andExpect(jsonPath("$.data.timeStamp").exists());
    }

    @Test
    @DisplayName("发起支付 - 订单ID不存在返回业务错误")
    void createPaymentWithInvalidOrderIdShouldReturnError() throws Exception {
        mockMvc.perform(post("/api/weixin/pay/create")
                .header("Authorization", createAuthHeader(1L))
                .contentType(JSON_MEDIA_TYPE)
                .content(createPaymentJson(99999L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("未登录查询订单列表 - 返回未授权")
    void getOrderListWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/orders/list"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录后查询我的订单列表 - 成功返回分页结果")
    void getOrderListAfterLoginShouldReturnPagedResult() throws Exception {
        mockMvc.perform(get("/api/weixin/orders/list")
                .header("Authorization", createAuthHeader(1L))
                .param("page_no", "1")
                .param("page_size", "10")
                .param("status", "ALL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.total").exists());
    }

    @Test
    @DisplayName("未登录查询已购产品 - 返回未授权")
    void getPurchasedProductsWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/orders/purchased-products"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录后查询已购产品 - 成功返回列表")
    void getPurchasedProductsAfterLoginShouldReturnList() throws Exception {
        mockMvc.perform(get("/api/weixin/orders/purchased-products")
                .header("Authorization", createAuthHeader(1L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("查询订单详情 - 非本人订单返回无权限")
    void getOrderDetailNotOwnedShouldReturnNoPermission() throws Exception {
        String loginResponse1 = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"code\":\"test-user-a-001\",\"nickname\":\"用户A\"}"))
            .andReturn().getResponse().getContentAsString();
        String accessToken1 = com.jayway.jsonpath.JsonPath.read(loginResponse1, "$.data.accessToken");

        String orderResponse = mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + accessToken1)
                .contentType(JSON_MEDIA_TYPE)
                .content(createFamilyCardOrderJson()))
            .andReturn().getResponse().getContentAsString();
        Long orderId = com.jayway.jsonpath.JsonPath.read(orderResponse, "$.data.orderId");

        mockMvc.perform(get("/api/weixin/orders/" + orderId)
                .header("Authorization", createAuthHeader(999L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("取消订单 - 本人待支付订单取消成功")
    void cancelPendingPaymentOrderShouldSucceed() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"code\":\"test-cancel-user-001\",\"nickname\":\"取消订单用户\"}"))
            .andReturn().getResponse().getContentAsString();
        String accessToken = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.data.accessToken");

        String orderResponse = mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(JSON_MEDIA_TYPE)
                .content(createFamilyCardOrderJson()))
            .andReturn().getResponse().getContentAsString();
        Long orderId = com.jayway.jsonpath.JsonPath.read(orderResponse, "$.data.orderId");

        mockMvc.perform(post("/api/weixin/orders/" + orderId + "/cancel")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
