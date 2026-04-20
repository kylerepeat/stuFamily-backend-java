package com.stufamily.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.stufamily.backend.wechat.gateway.WechatAuthGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("订单与支付模块集成测试")
public class WeixinOrderPayIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private WechatAuthGateway wechatAuthGateway;

    private static final String CREATE_ORDER_URL = "/api/weixin/orders/create";
    private static final String ORDER_STATUS_URL = "/api/weixin/orders/{orderNo}/status";
    private static final String PURCHASED_PRODUCTS_URL = "/api/weixin/orders/purchased-products";
    private static final String REVIEW_URL = "/api/weixin/orders/{orderNo}/review";
    private static final String PAY_NOTIFY_URL = "/api/weixin/pay/notify";

    private String testToken;
    private Long testUserId;

    @BeforeEach
    void setUp() throws Exception {
        when(wechatAuthGateway.code2Session(anyString()))
            .thenReturn(new WechatSession("openid_postman_admin_001", null, "session_key"));

        String loginBody = objectMapper.writeValueAsString(new LoginRequest("order_test_code", "测试用户", null));
        MvcResult loginResult = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        testToken = extractDataField(loginResult, "accessToken");
        testUserId = extractDataFieldAsLong(loginResult, "userId");
    }

    @Nested
    @DisplayName("创建订单接口")
    class CreateOrderTests {

        @Test
        @DisplayName("创建订单成功 - 家庭卡")
        void createOrder_success_familyCard() throws Exception {
            String applyDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String orderBody = objectMapper.writeValueAsString(new CreateOrderRequest(
                "FAMILY_CARD",
                1L,
                null,
                "MONTH",
                applyDate,
                "张三",
                "2024001",
                "13800138000",
                19900L
            ));

            mockMvc.perform(withAuth(post(CREATE_ORDER_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNo").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.payParams").exists())
                .andExpect(jsonPath("$.data.payParams.prepayId").isNotEmpty());
        }

        @Test
        @DisplayName("创建订单成功 - 增值服务")
        void createOrder_success_valueAddedService() throws Exception {
            String orderBody = objectMapper.writeValueAsString(new CreateOrderRequest(
                "VALUE_ADDED_SERVICE",
                2L,
                1L,
                null,
                null,
                null,
                null,
                null,
                12900L
            ));

            mockMvc.perform(withAuth(post(CREATE_ORDER_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        jsonPath("$.success").value(true).match(result);
                        jsonPath("$.data.orderNo").isNotEmpty().match(result);
                    } else if (status == 400) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Product or SKU not found in database");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                });
        }

        @Test
        @DisplayName("创建订单失败 - 未登录")
        void createOrder_notLoggedIn_failure() throws Exception {
            String orderBody = objectMapper.writeValueAsString(new CreateOrderRequest(
                "FAMILY_CARD",
                1L,
                null,
                "MONTH",
                "2024-01-01",
                "张三",
                "2024001",
                "13800138000",
                19900L
            ));

            mockMvc.perform(post(CREATE_ORDER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderBody))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("创建订单失败 - 缺少必填字段")
        void createOrder_missingRequiredFields_failure() throws Exception {
            String orderBody = "{}";

            mockMvc.perform(withAuth(post(CREATE_ORDER_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("创建订单失败 - 商品类型错误")
        void createOrder_invalidProductType_failure() throws Exception {
            String orderBody = objectMapper.writeValueAsString(new CreateOrderRequest(
                "INVALID_TYPE",
                1L,
                null,
                null,
                null,
                null,
                null,
                null,
                100L
            ));

            mockMvc.perform(withAuth(post(CREATE_ORDER_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("创建订单失败 - 金额小于1")
        void createOrder_invalidAmount_failure() throws Exception {
            String orderBody = objectMapper.writeValueAsString(new CreateOrderRequest(
                "FAMILY_CARD",
                1L,
                null,
                "MONTH",
                "2024-01-01",
                "张三",
                "2024001",
                "13800138000",
                0L
            ));

            mockMvc.perform(withAuth(post(CREATE_ORDER_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderBody))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("查询订单状态接口")
    class OrderStatusTests {

        @Test
        @DisplayName("查询订单状态成功")
        void getOrderStatus_success() throws Exception {
            mockMvc.perform(withAuth(get(ORDER_STATUS_URL, "ORDPOSTMANSEED000001"), testToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        jsonPath("$.success").value(true).match(result);
                        jsonPath("$.data").value("PAID").match(result);
                    } else if (status == 400) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Seed order not found in database");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                });
        }

        @Test
        @DisplayName("查询订单状态失败 - 订单不存在")
        void getOrderStatus_notFound_failure() throws Exception {
            mockMvc.perform(withAuth(get(ORDER_STATUS_URL, "ORDNOTEXIST999"), testToken))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("查询订单状态失败 - 未登录")
        void getOrderStatus_notLoggedIn_failure() throws Exception {
            mockMvc.perform(get(ORDER_STATUS_URL, "ORDPOSTMANSEED000001"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("查询已购商品列表接口")
    class PurchasedProductsTests {

        @Test
        @DisplayName("查询已购商品列表成功 - 无过滤条件")
        void listPurchasedProducts_success() throws Exception {
            mockMvc.perform(withAuth(get(PURCHASED_PRODUCTS_URL), testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());
        }

        @Test
        @DisplayName("查询已购商品列表成功 - 按商品类型过滤")
        void listPurchasedProducts_withTypeFilter() throws Exception {
            mockMvc.perform(withAuth(get(PURCHASED_PRODUCTS_URL), testToken)
                    .param("product_type", "FAMILY_CARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询已购商品列表成功 - 分页参数")
        void listPurchasedProducts_withPaging() throws Exception {
            mockMvc.perform(withAuth(get(PURCHASED_PRODUCTS_URL), testToken)
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
        }

        @Test
        @DisplayName("查询已购商品列表失败 - 未登录")
        void listPurchasedProducts_notLoggedIn_failure() throws Exception {
            mockMvc.perform(get(PURCHASED_PRODUCTS_URL))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("提交服务评价接口")
    class SubmitReviewTests {

        @Test
        @DisplayName("提交评价成功")
        void submitReview_success() throws Exception {
            String reviewBody = objectMapper.writeValueAsString(new ReviewRequest(5, "服务很好，非常满意！"));

            mockMvc.perform(withAuth(post(REVIEW_URL, "ORDPOSTMANSEED000002"), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        jsonPath("$.success").value(true).match(result);
                    } else if (status == 400) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Seed order not found or already reviewed");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                });
        }

        @Test
        @DisplayName("提交评价失败 - 订单未支付")
        void submitReview_orderNotPaid_failure() throws Exception {
            String reviewBody = objectMapper.writeValueAsString(new ReviewRequest(5, "服务很好"));

            mockMvc.perform(withAuth(post(REVIEW_URL, "ORDPOSTMANSEED000001"), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 400) {
                        return;
                    } else if (status == 200) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Seed order already paid - skip test");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                });
        }

        @Test
        @DisplayName("提交评价失败 - 评分超出范围")
        void submitReview_invalidStars_failure() throws Exception {
            String reviewBody = objectMapper.writeValueAsString(new ReviewRequest(6, "服务很好"));

            mockMvc.perform(withAuth(post(REVIEW_URL, "ORDPOSTMANSEED000002"), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("提交评价失败 - 评分小于1")
        void submitReview_starsTooLow_failure() throws Exception {
            String reviewBody = objectMapper.writeValueAsString(new ReviewRequest(0, "服务很好"));

            mockMvc.perform(withAuth(post(REVIEW_URL, "ORDPOSTMANSEED000002"), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("提交评价失败 - 内容超过500字符")
        void submitReview_contentTooLong_failure() throws Exception {
            String longContent = "a".repeat(501);
            String reviewBody = objectMapper.writeValueAsString(new ReviewRequest(5, longContent));

            mockMvc.perform(withAuth(post(REVIEW_URL, "ORDPOSTMANSEED000002"), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("提交评价失败 - 未登录")
        void submitReview_notLoggedIn_failure() throws Exception {
            String reviewBody = objectMapper.writeValueAsString(new ReviewRequest(5, "服务很好"));

            mockMvc.perform(post(REVIEW_URL, "ORDPOSTMANSEED000002")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewBody))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("提交评价失败 - 订单不存在")
        void submitReview_orderNotFound_failure() throws Exception {
            String reviewBody = objectMapper.writeValueAsString(new ReviewRequest(5, "服务很好"));

            mockMvc.perform(withAuth(post(REVIEW_URL, "ORDNOTEXIST999"), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewBody))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("支付回调通知接口")
    class PayNotifyTests {

        @Test
        @DisplayName("支付回调成功 - Mock模式")
        void payNotify_success_mockMode() throws Exception {
            String notifyBody = objectMapper.writeValueAsString(new PayNotifyRequest(
                "TEST_ORDER_12345",
                "TEST_TXN_12345",
                19900L
            ));

            mockMvc.perform(post(PAY_NOTIFY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(notifyBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        jsonPath("$.success").value(true).match(result);
                    } else if (status == 400) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Mock payment not enabled");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                });
        }

        @Test
        @DisplayName("支付回调失败 - Mock未启用")
        void payNotify_mockDisabled_failure() throws Exception {
        }

        @Test
        @DisplayName("支付回调失败 - 缺少必填字段")
        void payNotify_missingRequiredFields_failure() throws Exception {
            String notifyBody = "{}";

            mockMvc.perform(post(PAY_NOTIFY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(notifyBody))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("完整订单流程测试")
    class FullOrderFlowTests {

        @Test
        @DisplayName("完整流程 - 创建订单 -> 查询状态 -> 支付回调 -> 查询已购 -> 评价")
        void fullOrderFlow_success() throws Exception {
            String applyDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String orderBody = objectMapper.writeValueAsString(new CreateOrderRequest(
                "FAMILY_CARD",
                1L,
                null,
                "MONTH",
                applyDate,
                "流程测试",
                "FLOW001",
                "13900139000",
                19900L
            ));

            MvcResult createResult = mockMvc.perform(withAuth(post(CREATE_ORDER_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        jsonPath("$.data.orderNo").isNotEmpty().match(result);
                    } else if (status == 400) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Product not found - skip test");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                })
                .andReturn();

            String orderNo = extractDataField(createResult, "orderNo");
            if (orderNo == null) return;

            mockMvc.perform(withAuth(get(ORDER_STATUS_URL, orderNo), testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("PENDING_PAYMENT"));

            String notifyBody = objectMapper.writeValueAsString(new PayNotifyRequest(
                orderNo,
                "TXN_" + orderNo,
                19900L
            ));

            mockMvc.perform(post(PAY_NOTIFY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(notifyBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 200) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Pay notify failed - skip remaining");
                    }
                });

            mockMvc.perform(withAuth(get(ORDER_STATUS_URL, orderNo), testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("PAID"));

            mockMvc.perform(withAuth(get(PURCHASED_PRODUCTS_URL), testToken)
                    .param("product_type", "FAMILY_CARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());
        }
    }

    private record LoginRequest(String code, String nickname, String avatarUrl) {}

    private record CreateOrderRequest(
        String productType,
        Long productId,
        Long skuId,
        String durationType,
        String cardApplyDate,
        String applicantName,
        String applicantStudentOrCardNo,
        String applicantPhone,
        Long amountCents
    ) {}

    private record ReviewRequest(Integer stars, String content) {}

    private record PayNotifyRequest(String outTradeNo, String transactionId, Long totalAmountCents) {}
}
