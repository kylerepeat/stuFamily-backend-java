package com.stufamily.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

class WeixinPayIntegrationTest extends IntegrationTestBase {

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

    @Test
    void payNotifyMock_shouldSuccess_whenMockEnabled() throws Exception {
        String orderNo = createOrderAndGetOrderNo();

        mockMvc.perform(post("/api/weixin/pay/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "outTradeNo", orderNo,
                    "transactionId", "WX202604150001",
                    "totalAmountCents", 19900
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void payNotifyMock_shouldFail_whenAmountInvalid() throws Exception {
        mockMvc.perform(post("/api/weixin/pay/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "outTradeNo", "ORD202604150001",
                    "transactionId", "WX202604150001",
                    "totalAmountCents", 0
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void payNotifyMock_shouldFail_whenMissingRequiredField() throws Exception {
        mockMvc.perform(post("/api/weixin/pay/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "outTradeNo", "ORD202604150001"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void payNotifyXml_shouldReturnSuccessXml_whenValidPayload() throws Exception {
        String orderNo = createOrderAndGetOrderNo();
        String xmlPayload = "<xml>" +
            "<out_trade_no><![CDATA[" + orderNo + "]]></out_trade_no>" +
            "<transaction_id><![CDATA[WX202604150001]]></transaction_id>" +
            "<total_fee>19900</total_fee>" +
            "</xml>";

        mockMvc.perform(post("/api/weixin/pay/notify")
                .contentType(MediaType.APPLICATION_XML)
                .content(xmlPayload))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<return_code><![CDATA[SUCCESS]]></return_code>")));
    }

    @Test
    void payNotifyXml_shouldReturnFailXml_whenInvalidPayload() throws Exception {
        String xmlPayload = "<xml>" +
            "<invalid_field>value</invalid_field>" +
            "</xml>";

        mockMvc.perform(post("/api/weixin/pay/notify")
                .contentType(MediaType.APPLICATION_XML)
                .content(xmlPayload))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<return_code><![CDATA[FAIL]]></return_code>")));
    }

    @Test
    void payNotify_shouldBeAccessibleWithoutAuth() throws Exception {
        String orderNo = createOrderAndGetOrderNo();

        mockMvc.perform(post("/api/weixin/pay/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "outTradeNo", orderNo,
                    "transactionId", "WX202604150001",
                    "totalAmountCents", 19900
                ))))
            .andExpect(status().isOk());
    }
}
