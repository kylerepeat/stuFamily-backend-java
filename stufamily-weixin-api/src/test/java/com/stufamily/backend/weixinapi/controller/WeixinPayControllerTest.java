package com.stufamily.backend.weixinapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.order.application.service.OrderApplicationService;
import com.stufamily.backend.wechat.config.WechatProperties;
import com.stufamily.backend.wechat.gateway.WechatPayGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatPayNotifyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WeixinPayControllerTest {

    private MockMvc mockMvc;
    private OrderApplicationService orderApplicationService;
    private WechatPayGateway wechatPayGateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        orderApplicationService = Mockito.mock(OrderApplicationService.class);
        wechatPayGateway = Mockito.mock(WechatPayGateway.class);
        WechatProperties properties = new WechatProperties();
        properties.getPay().setMockNotifyEnabled(true);
        mockMvc = MockMvcBuilders.standaloneSetup(
            new WeixinPayController(orderApplicationService, wechatPayGateway, properties)).build();
    }

    @Test
    void notifyMockShouldReturnSuccess() throws Exception {
        doNothing().when(orderApplicationService).markPaid(any());
        mockMvc.perform(post("/api/weixin/pay/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Req("ORD1", "TX1", 100L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void notifyShouldReturnWechatXmlWhenSignatureIsValid() throws Exception {
        when(wechatPayGateway.parseOrderNotify(anyString()))
            .thenReturn(new WechatPayNotifyResult("ORD1", "TX1", 100L));
        doNothing().when(orderApplicationService).markPaid(any());

        mockMvc.perform(post("/api/weixin/pay/notify")
                .contentType(MediaType.APPLICATION_XML)
                .content("<xml><out_trade_no>ORD1</out_trade_no></xml>"))
            .andExpect(status().isOk())
            .andExpect(content().string("<xml><return_code><![CDATA[SUCCESS]]></return_code>"
                + "<return_msg><![CDATA[OK]]></return_msg></xml>"));
    }

    private record Req(String outTradeNo, String transactionId, long totalAmountCents) {
    }
}
