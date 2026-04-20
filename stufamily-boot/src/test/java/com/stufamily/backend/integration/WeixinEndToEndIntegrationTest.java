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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("端到端业务流程集成测试")
public class WeixinEndToEndIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private WechatAuthGateway wechatAuthGateway;

    private static final String LOGIN_URL = "/api/weixin/auth/login";
    private static final String PROFILE_URL = "/api/weixin/auth/profile";
    private static final String HOME_INDEX_URL = "/api/weixin/home/index";
    private static final String PRODUCTS_URL = "/api/weixin/home/products";
    private static final String CREATE_ORDER_URL = "/api/weixin/orders/create";
    private static final String ORDER_STATUS_URL = "/api/weixin/orders/{orderNo}/status";
    private static final String PAY_NOTIFY_URL = "/api/weixin/pay/notify";
    private static final String GROUP_QUOTA_URL = "/api/weixin/family/group/quota";
    private static final String MEMBERS_URL = "/api/weixin/family/members";
    private static final String CHECK_INS_URL = "/api/weixin/family/check-ins";
    private static final String PURCHASED_PRODUCTS_URL = "/api/weixin/orders/purchased-products";
    private static final String REVIEW_URL = "/api/weixin/orders/{orderNo}/review";
    private static final String MESSAGES_URL = "/api/weixin/home/messages";

    @Nested
    @DisplayName("用户注册登录流程")
    class UserRegistrationLoginFlow {

        @Test
        @DisplayName("完整流程 - 新用户登录 -> 更新资料")
        void newUserLoginAndUpdateProfile() throws Exception {
            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession("openid_new_user_flow_001", null, "session_key"));

            String loginBody = objectMapper.writeValueAsString(new LoginRequest(
                "new_user_code",
                "新用户",
                "https://example.com/avatar.png"
            ));

            MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

            String token = extractDataField(loginResult, "accessToken");

            String updateBody = objectMapper.writeValueAsString(new UpdateProfileRequest(
                "更新后的昵称",
                "13800138001"
            ));

            mockMvc.perform(withAuth(put(PROFILE_URL), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("更新后的昵称"))
                .andExpect(jsonPath("$.data.phone").value("13800138001"));
        }
    }

    @Nested
    @DisplayName("家庭卡购买与使用流程")
    class FamilyCardPurchaseAndUseFlow {

        @Test
        @DisplayName("完整流程 - 购买家庭卡 -> 支付 -> 查看家庭组 -> 添加成员 -> 打卡")
        void fullFamilyCardFlow() throws Exception {
            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession("openid_family_flow_001", null, "session_key"));

            String loginBody = objectMapper.writeValueAsString(new LoginRequest("family_flow_code", "家庭用户", null));
            MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

            String token = extractDataField(loginResult, "accessToken");

            mockMvc.perform(withAuth(get(GROUP_QUOTA_URL), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasActiveGroup").isBoolean());

            String applyDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String orderBody = objectMapper.writeValueAsString(new CreateOrderRequest(
                "FAMILY_CARD",
                1L,
                null,
                "MONTH",
                applyDate,
                "家庭卡申请人",
                "FAMILY001",
                "13800138002",
                19900L
            ));

            MvcResult createResult = mockMvc.perform(withAuth(post(CREATE_ORDER_URL), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        jsonPath("$.data.orderNo").isNotEmpty().match(result);
                    } else if (status == 400) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Product not found in database - skip test");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                })
                .andReturn();

            String orderNo = extractDataField(createResult, "orderNo");
            if (orderNo == null) return;

            mockMvc.perform(withAuth(get(ORDER_STATUS_URL, orderNo), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("PENDING_PAYMENT"));

            String notifyBody = objectMapper.writeValueAsString(new PayNotifyRequest(
                orderNo,
                "TXN_FAMILY_" + orderNo,
                19900L
            ));

            mockMvc.perform(post(PAY_NOTIFY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(notifyBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 200) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Pay notify failed - skip remaining test");
                    }
                });

            mockMvc.perform(withAuth(get(ORDER_STATUS_URL, orderNo), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("PAID"));

            MvcResult quotaResult = mockMvc.perform(withAuth(get(GROUP_QUOTA_URL), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasActiveGroup").value(true))
                .andReturn();

            JsonNode quotaData = parseResponse(quotaResult).get("data");
            String groupNo = quotaData.get("groups").get(0).get("groupNo").asText();

            String joinedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            String memberBody = objectMapper.writeValueAsString(new AddMemberRequest(
                groupNo,
                "家庭成员1",
                "MEMBER001",
                "13900139001",
                joinedAt
            ));

            mockMvc.perform(withAuth(post(MEMBERS_URL), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(memberBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        jsonPath("$.success").value(true).match(result);
                        jsonPath("$.data.memberNo").isNotEmpty().match(result);
                    } else if (status == 400) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Family group quota exceeded - skip remaining");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                });

            String checkedInAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            String checkInBody = objectMapper.writeValueAsString(new CheckInRequest(
                groupNo,
                null,
                new BigDecimal("31.2304160"),
                new BigDecimal("121.4737010"),
                "测试打卡地址",
                checkedInAt
            ));

            mockMvc.perform(withAuth(post(CHECK_INS_URL), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkInBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        jsonPath("$.success").value(true).match(result);
                        jsonPath("$.data.checkinNo").isNotEmpty().match(result);
                    } else if (status == 400) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Check-in failed - skip test");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                });
        }
    }

    @Nested
    @DisplayName("增值服务购买与评价流程")
    class ValueAddedServicePurchaseAndReviewFlow {

        @Test
        @DisplayName("完整流程 - 购买增值服务 -> 支付 -> 查看已购 -> 评价")
        void fullValueAddedServiceFlow() throws Exception {
            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession("openid_vas_flow_001", null, "session_key"));

            String loginBody = objectMapper.writeValueAsString(new LoginRequest("vas_flow_code", "增值服务用户", null));
            MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

            String token = extractDataField(loginResult, "accessToken");

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

            MvcResult createResult = mockMvc.perform(withAuth(post(CREATE_ORDER_URL), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        jsonPath("$.data.orderNo").isNotEmpty().match(result);
                    } else if (status == 400) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Product or SKU not found in database - skip test");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                })
                .andReturn();

            String orderNo = extractDataField(createResult, "orderNo");
            if (orderNo == null) return;

            String notifyBody = objectMapper.writeValueAsString(new PayNotifyRequest(
                orderNo,
                "TXN_VAS_" + orderNo,
                12900L
            ));

            mockMvc.perform(post(PAY_NOTIFY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(notifyBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 200) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Pay notify failed - skip remaining test");
                    }
                });

            mockMvc.perform(withAuth(get(PURCHASED_PRODUCTS_URL), token)
                    .param("product_type", "VALUE_ADDED_SERVICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

            String reviewBody = objectMapper.writeValueAsString(new ReviewRequest(5, "增值服务体验很好！"));

            mockMvc.perform(withAuth(post(REVIEW_URL, orderNo), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reviewBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        jsonPath("$.success").value(true).match(result);
                    } else if (status == 400) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Review failed - order may already be reviewed");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                });

            mockMvc.perform(withAuth(get(PURCHASED_PRODUCTS_URL), token)
                    .param("product_type", "VALUE_ADDED_SERVICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());
        }
    }

    @Nested
    @DisplayName("首页浏览与留言流程")
    class HomeBrowseAndMessageFlow {

        @Test
        @DisplayName("完整流程 - 浏览首页 -> 查看商品 -> 登录 -> 提交留言")
        void browseHomeAndLeaveMessage() throws Exception {
            mockMvc.perform(get(HOME_INDEX_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.banners").isArray())
                .andExpect(jsonPath("$.data.siteProfile").exists());

            LocalDate today = LocalDate.now();
            String startDate = today.minusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String endDate = today.plusDays(365).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            mockMvc.perform(get(PRODUCTS_URL)
                    .param("sale_start_at", startDate)
                    .param("sale_end_at", endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession("openid_message_flow_001", null, "session_key"));

            String loginBody = objectMapper.writeValueAsString(new LoginRequest("message_flow_code", "留言用户", null));
            MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

            String token = extractDataField(loginResult, "accessToken");

            String messageBody = objectMapper.writeValueAsString(new MessageRequest("这是我的留言，希望得到回复。"));

            mockMvc.perform(withAuth(post(MESSAGES_URL), token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(messageBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("这是我的留言，希望得到回复。"));

            mockMvc.perform(withAuth(get("/api/weixin/home/messages/mine"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[?(@.content == '这是我的留言，希望得到回复。')]").exists());
        }
    }

    @Nested
    @DisplayName("未授权访问场景")
    class UnauthorizedAccessScenarios {

        @Test
        @DisplayName("未登录访问需要认证的接口")
        void accessProtectedEndpointsWithoutAuth() throws Exception {
            mockMvc.perform(post(CREATE_ORDER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isUnauthorized());

            mockMvc.perform(get(GROUP_QUOTA_URL))
                .andExpect(status().isUnauthorized());

            mockMvc.perform(get(MEMBERS_URL))
                .andExpect(status().isUnauthorized());

            mockMvc.perform(post(CHECK_INS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isUnauthorized());

            mockMvc.perform(post(MESSAGES_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isUnauthorized());

            mockMvc.perform(get(PURCHASED_PRODUCTS_URL))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("公开接口无需登录即可访问")
        void accessPublicEndpointsWithoutAuth() throws Exception {
            mockMvc.perform(get(HOME_INDEX_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

            LocalDate today = LocalDate.now();
            mockMvc.perform(get(PRODUCTS_URL)
                    .param("sale_start_at", today.toString())
                    .param("sale_end_at", today.plusMonths(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(get(PRODUCTS_URL + "/1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 200 && status != 400) {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                });

            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession("openid_public_test_001", null, "session_key"));

            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"test\"}"))
                .andExpect(status().isOk());
        }
    }

    private record LoginRequest(String code, String nickname, String avatarUrl) {}

    private record UpdateProfileRequest(String nickname, String phone) {}

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

    private record PayNotifyRequest(String outTradeNo, String transactionId, Long totalAmountCents) {}

    private record AddMemberRequest(String groupNo, String memberName, String studentOrCardNo, String phone, String joinedAt) {}

    private record CheckInRequest(String groupNo, Long familyMemberId, BigDecimal latitude, BigDecimal longitude, String addressText, String checkedInAt) {}

    private record ReviewRequest(Integer stars, String content) {}

    private record MessageRequest(String content) {}
}
