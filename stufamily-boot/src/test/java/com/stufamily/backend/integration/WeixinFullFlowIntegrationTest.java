package com.stufamily.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeixinFullFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void fullFlow_loginAndAccessProtectedResources() throws Exception {
        String token = loginAndGetToken("flow_test_code", "流程测试用户", "https://example.com/flow.png");

        mockMvc.perform(get("/api/weixin/home/messages/mine")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/weixin/family/group/quota")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.hasActiveGroup").value(false));
    }

    @Test
    void fullFlow_createOrderAndCheckStatus() throws Exception {
        String token = loginAndGetToken("order_flow_code", "订单测试用户", "https://example.com/order.png");

        MvcResult orderResult = mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "productType", "FAMILY_CARD",
                    "productId", 1001,
                    "durationType", "YEAR",
                    "cardApplyDate", "2026-04-15",
                    "applicantName", "王五",
                    "applicantStudentOrCardNo", "20260005",
                    "applicantPhone", "13800138005",
                    "amountCents", 19900
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderNo").exists())
            .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
            .andReturn();

        String response = orderResult.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(response, Map.class);
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        String orderNo = (String) data.get("orderNo");

        mockMvc.perform(get("/api/weixin/orders/{orderNo}/status", orderNo)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("PENDING_PAYMENT"));
    }

    @Test
    void fullFlow_submitMessageAndQuery() throws Exception {
        String token = loginAndGetToken("message_flow_code", "留言测试用户", "https://example.com/message.png");

        mockMvc.perform(post("/api/weixin/home/messages")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "content", "这是一条流程测试留言"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").value("这是一条流程测试留言"));

        mockMvc.perform(get("/api/weixin/home/messages/mine")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].content").value("这是一条流程测试留言"));
    }

    @Test
    void fullFlow_updateProfileAndVerify() throws Exception {
        String token = loginAndGetToken("profile_flow_code", "资料测试用户", "https://example.com/profile.png");

        mockMvc.perform(put("/api/weixin/auth/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "nickname", "更新后的昵称",
                    "phone", "13999999999"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nickname").value("更新后的昵称"))
            .andExpect(jsonPath("$.data.phone").value("13999999999"));
    }

    @Test
    void fullFlow_adminCanAccessWeixinEndpoints() throws Exception {
        mockMvc.perform(get("/api/weixin/home/messages/mine")
                .header("Authorization", "Bearer " + adminAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/weixin/family/group/quota")
                .header("Authorization", "Bearer " + adminAccessToken))
            .andExpect(status().isOk());
    }

    @Test
    void fullFlow_publicEndpointsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/weixin/home/index"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/weixin/home/products")
                .param("sale_start_at", "2026-01-01")
                .param("sale_end_at", "2026-12-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/weixin/home/products/1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void fullFlow_protectedEndpointsRequireAuth() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "productType", "FAMILY_CARD",
                    "productId", 1001,
                    "durationType", "YEAR",
                    "cardApplyDate", "2026-04-15",
                    "applicantName", "测试",
                    "applicantStudentOrCardNo", "20260001",
                    "applicantPhone", "13800138000",
                    "amountCents", 19900
                ))))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/weixin/home/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "content", "测试留言"
                ))))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/weixin/orders/purchased-products"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/weixin/family/members"))
            .andExpect(status().isUnauthorized());
    }
}
