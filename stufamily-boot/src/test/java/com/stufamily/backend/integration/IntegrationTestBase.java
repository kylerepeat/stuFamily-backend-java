package com.stufamily.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.boot.StuFamilyBackendApplication;
import com.stufamily.backend.integration.config.TestConfig;
import com.stufamily.backend.shared.security.JwtTokenProvider;
import com.stufamily.backend.shared.security.AuthAudience;
import com.stufamily.backend.wechat.gateway.WechatAuthGateway;
import com.stufamily.backend.wechat.gateway.WechatPayGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateRequest;
import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateResponse;
import com.stufamily.backend.wechat.gateway.dto.WechatPayNotifyResult;
import com.stufamily.backend.wechat.gateway.dto.WechatPayRefundRequest;
import com.stufamily.backend.wechat.gateway.dto.WechatPayRefundResponse;
import com.stufamily.backend.wechat.gateway.dto.WechatSession;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StuFamilyBackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
public abstract class IntegrationTestBase {

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

    protected String wechatAccessToken;
    protected String adminAccessToken;

    @BeforeEach
    void setUpBase() {
        wechatAccessToken = createWechatToken(1001L, "test_wechat_user");
        adminAccessToken = createAdminToken(1999L, "admin");
        mockWechatGateways();
    }

    protected void mockWechatGateways() {
        when(wechatAuthGateway.code2Session(any())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            String openid = "test_openid_" + Math.abs(code.hashCode());
            return new WechatSession(openid, "test_unionid", "test_session_key");
        });

        when(wechatPayGateway.createMiniappOrder(any())).thenAnswer(invocation -> {
            WechatPayCreateRequest request = invocation.getArgument(0);
            return new WechatPayCreateResponse(
                "mock_prepay_" + request.outTradeNo(),
                UUID.randomUUID().toString().replace("-", ""),
                "mock_sign",
                String.valueOf(System.currentTimeMillis() / 1000)
            );
        });

        when(wechatPayGateway.parseOrderNotify(any())).thenAnswer(invocation -> {
            String payload = invocation.getArgument(0);
            // 从XML中提取out_trade_no
            String outTradeNo = "ORD202604150001";
            if (payload.contains("<out_trade_no>")) {
                int start = payload.indexOf("<out_trade_no>") + "<out_trade_no>".length();
                int end = payload.indexOf("</out_trade_no>", start);
                if (end > start) {
                    outTradeNo = payload.substring(start, end);
                    // 处理CDATA
                    if (outTradeNo.startsWith("<![CDATA[")) {
                        outTradeNo = outTradeNo.substring(9, outTradeNo.length() - 3);
                    }
                }
            }
            return new WechatPayNotifyResult(outTradeNo, "WX202604150001", 19900L);
        });

        when(wechatPayGateway.refundOrder(any())).thenAnswer(invocation -> {
            WechatPayRefundRequest request = invocation.getArgument(0);
            return new WechatPayRefundResponse(
                "mock_refund_" + request.outRefundNo(),
                "SUCCESS",
                "SUCCESS",
                "SUCCESS",
                null,
                null
            );
        });
    }

    protected String createWechatToken(Long userId, String username) {
        return jwtTokenProvider.createAccessToken(userId, username, List.of("WECHAT"), AuthAudience.WEIXIN, 0L);
    }

    protected String createAdminToken(Long userId, String username) {
        return jwtTokenProvider.createAccessToken(userId, username, List.of("ADMIN"), AuthAudience.ADMIN, 0L);
    }

    protected String loginAndGetToken(String code, String nickname, String avatarUrl) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "code", code,
                    "nickname", nickname,
                    "avatarUrl", avatarUrl
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(response, Map.class);
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        return (String) data.get("accessToken");
    }
}
