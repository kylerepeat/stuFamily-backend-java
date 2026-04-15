package com.stufamily.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeixinOrderIntegrationTest extends IntegrationTestBase {

    @Test
    void createOrder_shouldSuccess_whenValidFamilyCardOrder() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "productType", "FAMILY_CARD",
                    "productId", 1001,
                    "durationType", "YEAR",
                    "cardApplyDate", "2026-04-15",
                    "applicantName", "张三",
                    "applicantStudentOrCardNo", "20260001",
                    "applicantPhone", "13800138000",
                    "amountCents", 19900
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderNo").exists())
            .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.data.payableAmountCents").value(19900))
            .andExpect(jsonPath("$.data.payParams.prepayId").exists());
    }

    @Test
    void createOrder_shouldFail_whenMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "productType", "FAMILY_CARD",
                    "productId", 1001
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void createOrder_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "productType", "FAMILY_CARD",
                    "productId", 1001,
                    "durationType", "YEAR",
                    "cardApplyDate", "2026-04-15",
                    "applicantName", "张三",
                    "applicantStudentOrCardNo", "20260001",
                    "applicantPhone", "13800138000",
                    "amountCents", 19900
                ))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_shouldFail_whenInvalidProductType() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "productType", "INVALID_TYPE",
                    "productId", 1001,
                    "amountCents", 100
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void orderStatus_shouldReturnStatus_whenOrderExists() throws Exception {
        String orderNo = createOrderAndGetOrderNo();

        mockMvc.perform(get("/api/weixin/orders/{orderNo}/status", orderNo)
                .header("Authorization", "Bearer " + wechatAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value("PENDING_PAYMENT"));
    }

    @Test
    void orderStatus_shouldFail_whenOrderNotFound() throws Exception {
        mockMvc.perform(get("/api/weixin/orders/{orderNo}/status", "ORD_NONEXISTENT")
                .header("Authorization", "Bearer " + wechatAccessToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void purchasedProducts_shouldReturnEmptyList_whenNoPurchases() throws Exception {
        mockMvc.perform(get("/api/weixin/orders/purchased-products")
                .header("Authorization", "Bearer " + wechatAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void purchasedProducts_shouldFilterByProductType_whenTypeProvided() throws Exception {
        mockMvc.perform(get("/api/weixin/orders/purchased-products")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .param("product_type", "FAMILY_CARD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void purchasedProducts_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/orders/purchased-products"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void submitReview_shouldFail_whenOrderNotPaid() throws Exception {
        String orderNo = createOrderAndGetOrderNo();

        mockMvc.perform(post("/api/weixin/orders/{orderNo}/review", orderNo)
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "stars", 5,
                    "content", "非常满意"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void submitReview_shouldFail_whenInvalidStars() throws Exception {
        String orderNo = createOrderAndGetOrderNo();

        mockMvc.perform(post("/api/weixin/orders/{orderNo}/review", orderNo)
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "stars", 6,
                    "content", "评价内容"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void submitReview_shouldFail_whenStarsTooLow() throws Exception {
        String orderNo = createOrderAndGetOrderNo();

        mockMvc.perform(post("/api/weixin/orders/{orderNo}/review", orderNo)
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "stars", 0,
                    "content", "评价内容"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void submitReview_shouldFail_whenContentTooLong() throws Exception {
        String orderNo = createOrderAndGetOrderNo();
        String longContent = "a".repeat(501);

        mockMvc.perform(post("/api/weixin/orders/{orderNo}/review", orderNo)
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "stars", 5,
                    "content", longContent
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void submitReview_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/{orderNo}/review", "ORD123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "stars", 5,
                    "content", "评价"
                ))))
            .andExpect(status().isUnauthorized());
    }

    private String createOrderAndGetOrderNo() throws Exception {
        var result = mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "productType", "FAMILY_CARD",
                    "productId", 1001,
                    "durationType", "YEAR",
                    "cardApplyDate", "2026-04-15",
                    "applicantName", "张三",
                    "applicantStudentOrCardNo", "20260001",
                    "applicantPhone", "13800138000",
                    "amountCents", 19900
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderNo").exists())
            .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(response, Map.class);
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        return (String) data.get("orderNo");
    }
}
