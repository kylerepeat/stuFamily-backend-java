package com.stufamily.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.shared.security.AuthAudience;
import com.stufamily.backend.shared.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

@IntegrationTest
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    protected String generateWechatToken(Long userId, String username) {
        return jwtTokenProvider.createAccessToken(userId, username, List.of("WECHAT"), AuthAudience.WEIXIN);
    }

    protected String generateAdminToken(Long userId, String username) {
        return jwtTokenProvider.createAccessToken(userId, username, List.of("ADMIN"), AuthAudience.ADMIN);
    }

    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    protected JsonNode parseResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected String extractDataField(MvcResult result, String field) throws Exception {
        JsonNode root = parseResponse(result);
        JsonNode data = root.get("data");
        if (data == null || data.isNull()) {
            return null;
        }
        JsonNode fieldNode = data.get(field);
        return fieldNode == null || fieldNode.isNull() ? null : fieldNode.asText();
    }

    protected Long extractDataFieldAsLong(MvcResult result, String field) throws Exception {
        JsonNode root = parseResponse(result);
        JsonNode data = root.get("data");
        if (data == null || data.isNull()) {
            return null;
        }
        JsonNode fieldNode = data.get(field);
        return fieldNode == null || fieldNode.isNull() ? null : fieldNode.asLong();
    }
}
