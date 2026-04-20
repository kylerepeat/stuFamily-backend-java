package com.stufamily.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.shared.security.JwtTokenProvider;
import com.stufamily.backend.wechat.gateway.WechatAuthGateway;
import com.stufamily.backend.wechat.gateway.WechatPayGateway;
import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateResponse;
import com.stufamily.backend.wechat.gateway.dto.WechatSession;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.stufamily.backend.boot.StuFamilyBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WeixinFamilyIntegrationTest {

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

    protected static final MediaType JSON_MEDIA_TYPE = MediaType.APPLICATION_JSON;

    @BeforeEach
    void setUpBase() {
        WechatSession mockSession = new WechatSession("test-openid-123", "test-unionid-123", "test-session-key");
        when(wechatAuthGateway.code2Session(any())).thenReturn(mockSession);

        WechatPayCreateResponse mockPayResponse = new WechatPayCreateResponse(
            "test-prepay-id-123",
            "test-nonce-str",
            "test-pay-sign",
            String.valueOf(System.currentTimeMillis() / 1000)
        );
        when(wechatPayGateway.createMiniappOrder(any())).thenReturn(mockPayResponse);
    }

    protected String createWechatUserToken(Long userId) {
        return jwtTokenProvider.createAccessToken(
            userId,
            "test-user-" + userId,
            List.of("WEIXIN"),
            com.stufamily.backend.shared.security.AuthAudience.WEIXIN
        );
    }

    protected String createAuthHeader(Long userId) {
        return "Bearer " + createWechatUserToken(userId);
    }

    @Test
    @DisplayName("未登录查询家庭组配额 - 返回未授权")
    void getGroupQuotaWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/family/group/quota"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录后查询家庭组配额 - 成功返回")
    void getGroupQuotaAfterLoginShouldReturnData() throws Exception {
        mockMvc.perform(get("/api/weixin/family/group/quota")
                .header("Authorization", createAuthHeader(1L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.maxMembers").exists())
            .andExpect(jsonPath("$.data.currentMembers").exists());
    }

    @Test
    @DisplayName("未登录查询家庭成员列表 - 返回未授权")
    void getFamilyMembersWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/family/members"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录后查询家庭成员列表 - 成功返回列表")
    void getFamilyMembersAfterLoginShouldReturnList() throws Exception {
        mockMvc.perform(get("/api/weixin/family/members")
                .header("Authorization", createAuthHeader(1L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("未登录添加家庭成员 - 返回未授权")
    void addMemberWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/weixin/family/members")
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"name\":\"测试成员\",\"relationship\":\"子女\",\"birthDate\":\"2020-01-01\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录后添加家庭成员 - 添加成功")
    void addMemberAfterLoginShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/weixin/family/members")
                .header("Authorization", createAuthHeader(1L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"name\":\"小明\",\"relationship\":\"儿子\",\"birthDate\":\"2020-01-15\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.name").value("小明"))
            .andExpect(jsonPath("$.data.relationship").value("儿子"));
    }

    @Test
    @DisplayName("添加家庭成员 - 姓名为空返回参数错误")
    void addMemberWithEmptyNameShouldReturnInvalidParam() throws Exception {
        mockMvc.perform(post("/api/weixin/family/members")
                .header("Authorization", createAuthHeader(1L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"name\":\"\",\"relationship\":\"子女\",\"birthDate\":\"2020-01-01\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    @DisplayName("添加家庭成员 - 关系为空返回参数错误")
    void addMemberWithEmptyRelationshipShouldReturnInvalidParam() throws Exception {
        mockMvc.perform(post("/api/weixin/family/members")
                .header("Authorization", createAuthHeader(1L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"name\":\"测试成员\",\"relationship\":\"\",\"birthDate\":\"2020-01-01\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    @DisplayName("添加家庭成员 - 出生日期格式错误返回参数错误")
    void addMemberWithInvalidBirthDateFormatShouldReturnInvalidParam() throws Exception {
        mockMvc.perform(post("/api/weixin/family/members")
                .header("Authorization", createAuthHeader(1L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"name\":\"测试成员\",\"relationship\":\"子女\",\"birthDate\":\"invalid-date\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    @DisplayName("未登录删除家庭成员 - 返回未授权")
    void deleteMemberWithoutLoginShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/weixin/family/members/1"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录后删除家庭成员 - 删除成功")
    void deleteMemberAfterLoginShouldSucceed() throws Exception {
        String addResponse = mockMvc.perform(post("/api/weixin/family/members")
                .header("Authorization", createAuthHeader(2L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"name\":\"待删除成员\",\"relationship\":\"子女\",\"birthDate\":\"2020-01-01\"}"))
            .andReturn().getResponse().getContentAsString();

        Long memberId = com.jayway.jsonpath.JsonPath.read(addResponse, "$.data.id");

        mockMvc.perform(delete("/api/weixin/family/members/" + memberId)
                .header("Authorization", createAuthHeader(2L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("删除不存在的家庭成员 - 返回业务错误")
    void deleteNonExistentMemberShouldReturnError() throws Exception {
        mockMvc.perform(delete("/api/weixin/family/members/99999")
                .header("Authorization", createAuthHeader(1L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("删除非本人家庭成员 - 返回无权限")
    void deleteOtherUserMemberShouldReturnNoPermission() throws Exception {
        String addResponse = mockMvc.perform(post("/api/weixin/family/members")
                .header("Authorization", createAuthHeader(3L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"name\":\"用户3的成员\",\"relationship\":\"子女\",\"birthDate\":\"2020-01-01\"}"))
            .andReturn().getResponse().getContentAsString();

        Long memberId = com.jayway.jsonpath.JsonPath.read(addResponse, "$.data.id");

        mockMvc.perform(delete("/api/weixin/family/members/" + memberId)
                .header("Authorization", createAuthHeader(4L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("查询家庭组统计 - 登录后成功返回")
    void getFamilyStatsAfterLoginShouldReturnData() throws Exception {
        mockMvc.perform(post("/api/weixin/family/members")
                .header("Authorization", createAuthHeader(5L))
                .contentType(JSON_MEDIA_TYPE)
                .content("{\"name\":\"统计测试成员\",\"relationship\":\"子女\",\"birthDate\":\"2020-01-01\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/weixin/family/stats")
                .header("Authorization", createAuthHeader(5L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalMembers").exists())
            .andExpect(jsonPath("$.data.activeServices").exists());
    }
}
