package com.stufamily.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeixinHomeIntegrationTest extends IntegrationTestBase {

    @Test
    void index_shouldReturnHomePage_whenNoAuth() throws Exception {
        mockMvc.perform(get("/api/weixin/home/index"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.banners").isArray())
            .andExpect(jsonPath("$.data.siteProfile.communityName").exists())
            .andExpect(jsonPath("$.data.notices").isArray());
    }

    @Test
    void products_shouldReturnProductList_whenValidDateRange() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        mockMvc.perform(get("/api/weixin/home/products")
                .param("sale_start_at", today.format(formatter))
                .param("sale_end_at", nextMonth.format(formatter)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void products_shouldFail_whenMissingDateParams() throws Exception {
        mockMvc.perform(get("/api/weixin/home/products"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void productDetail_shouldReturnDetail_whenValidProductId() throws Exception {
        mockMvc.perform(get("/api/weixin/home/products/1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1001))
            .andExpect(jsonPath("$.data.title").exists())
            .andExpect(jsonPath("$.data.type").exists())
            .andExpect(jsonPath("$.data.familyCardPlans").isArray());
    }

    @Test
    void productDetail_shouldFail_whenProductNotFound() throws Exception {
        mockMvc.perform(get("/api/weixin/home/products/99999"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void createMessage_shouldSuccess_whenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/weixin/home/messages")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "content", "这是一条测试留言"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").value("这是一条测试留言"))
            .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createMessage_shouldFail_whenContentTooLong() throws Exception {
        String longContent = "a".repeat(501);
        mockMvc.perform(post("/api/weixin/home/messages")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "content", longContent
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void createMessage_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/weixin/home/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "content", "测试留言"
                ))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void myMessages_shouldReturnList_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/weixin/home/messages/mine")
                .header("Authorization", "Bearer " + wechatAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.total").exists())
            .andExpect(jsonPath("$.data.pageNo").exists());
    }

    @Test
    void myMessages_shouldReturnPagedResult_whenWithPaginationParams() throws Exception {
        mockMvc.perform(get("/api/weixin/home/messages/mine")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .param("page_no", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test
    void myMessages_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/home/messages/mine"))
            .andExpect(status().isUnauthorized());
    }
}
