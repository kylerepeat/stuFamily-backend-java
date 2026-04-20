package com.stufamily.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeixinErrorScenarioIntegrationTest extends IntegrationTestBase {

    @Test
    void error_expiredTokenShouldReturnUnauthorized() throws Exception {
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJ0ZXN0Iiwicm9sZXMiOlsiV0VDSEFUIl0sImF1ZGllbmNlIjoiV0VJWElOIiwiZXhwIjoxNTE2MjM5MDIyfQ.invalid_signature";

        mockMvc.perform(get("/api/weixin/home/messages/mine")
                .header("Authorization", "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void error_malformedTokenShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/home/messages/mine")
                .header("Authorization", "Bearer malformed.token.here"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void error_missingBearerPrefixShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/home/messages/mine")
                .header("Authorization", wechatAccessToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void error_invalidJsonShouldReturnError() throws Exception {
        mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
            .andExpect(status().is5xxServerError());
    }

    @Test
    void error_emptyRequestBodyShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void error_createOrderWithInvalidAmount_shouldFail() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "productType", "FAMILY_CARD",
                    "productId", 1001,
                    "durationType", "YEAR",
                    "cardApplyDate", "2026-04-15",
                    "applicantName", "测试",
                    "applicantStudentOrCardNo", "20260001",
                    "applicantPhone", "13800138000",
                    "amountCents", -100
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void error_createOrderWithZeroAmount_shouldFail() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/create")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "productType", "FAMILY_CARD",
                    "productId", 1001,
                    "durationType", "YEAR",
                    "cardApplyDate", "2026-04-15",
                    "applicantName", "测试",
                    "applicantStudentOrCardNo", "20260001",
                    "applicantPhone", "13800138000",
                    "amountCents", 0
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void error_invalidPhoneFormat_shouldFail() throws Exception {
        mockMvc.perform(put("/api/weixin/auth/profile")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "nickname", "测试用户",
                    "phone", "02345678901"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void error_phoneWithLetters_shouldFail() throws Exception {
        mockMvc.perform(put("/api/weixin/auth/profile")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "nickname", "测试用户",
                    "phone", "1380013800a"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void error_messageWithEmptyContent_shouldFail() throws Exception {
        mockMvc.perform(post("/api/weixin/home/messages")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "content", ""
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void error_invalidDateFormat_shouldFail() throws Exception {
        mockMvc.perform(get("/api/weixin/home/products")
                .param("sale_start_at", "invalid-date")
                .param("sale_end_at", "2026-12-31"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void error_nonexistentProduct_shouldFail() throws Exception {
        mockMvc.perform(get("/api/weixin/home/products/999999"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void error_reviewNonExistentOrder_shouldFail() throws Exception {
        mockMvc.perform(post("/api/weixin/orders/{orderNo}/review", "ORD_NONEXISTENT")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "stars", 5,
                    "content", "好评"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void error_cancelNonExistentMember_shouldFail() throws Exception {
        mockMvc.perform(delete("/api/weixin/family/members/{memberNo}", "M_NONEXISTENT_999")
                .header("Authorization", "Bearer " + wechatAccessToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }
}
