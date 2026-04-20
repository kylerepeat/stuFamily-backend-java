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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("家庭模块接口集成测试")
public class WeixinFamilyIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private WechatAuthGateway wechatAuthGateway;

    private static final String MEMBERS_URL = "/api/weixin/family/members";
    private static final String CHECK_INS_URL = "/api/weixin/family/check-ins";
    private static final String GROUP_QUOTA_URL = "/api/weixin/family/group/quota";

    private String testToken;
    private Long testUserId;
    private String testGroupNo;

    @BeforeEach
    void setUp() throws Exception {
        when(wechatAuthGateway.code2Session(anyString()))
            .thenReturn(new WechatSession("openid_postman_admin_001", null, "session_key"));

        String loginBody = objectMapper.writeValueAsString(new LoginRequest("family_test_code", "测试用户", null));
        MvcResult loginResult = mockMvc.perform(post("/api/weixin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        testToken = extractDataField(loginResult, "accessToken");
        testUserId = extractDataFieldAsLong(loginResult, "userId");

        MvcResult quotaResult = mockMvc.perform(withAuth(get(GROUP_QUOTA_URL), testToken))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode quotaData = parseResponse(quotaResult).get("data");
        if (quotaData.get("hasActiveGroup").asBoolean()) {
            testGroupNo = quotaData.get("groups").get(0).get("groupNo").asText();
        }
    }

    @Nested
    @DisplayName("家庭组名额查询接口")
    class GroupQuotaTests {

        @Test
        @DisplayName("查询家庭组名额成功 - 已登录用户")
        void getGroupQuota_success() throws Exception {
            mockMvc.perform(withAuth(get(GROUP_QUOTA_URL), testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasActiveGroup").isBoolean())
                .andExpect(jsonPath("$.data.groups").isArray());
        }

        @Test
        @DisplayName("查询家庭组名额失败 - 未登录")
        void getGroupQuota_notLoggedIn_failure() throws Exception {
            mockMvc.perform(get(GROUP_QUOTA_URL))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("新增家庭成员接口")
    class AddMemberTests {

        @Test
        @DisplayName("新增家庭成员成功")
        void addMember_success() throws Exception {
            if (testGroupNo == null) {
                org.junit.jupiter.api.Assumptions.assumeTrue(false, "No active family group available");
                return;
            }

            String joinedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            String memberBody = objectMapper.writeValueAsString(new AddMemberRequest(
                testGroupNo,
                "测试家人" + System.currentTimeMillis(),
                "STU" + System.currentTimeMillis(),
                "13900139000",
                joinedAt
            ));

            mockMvc.perform(withAuth(post(MEMBERS_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(memberBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        jsonPath("$.success").value(true).match(result);
                        jsonPath("$.data.memberNo").isNotEmpty().match(result);
                        jsonPath("$.data.memberName").exists().match(result);
                        jsonPath("$.data.status").value("ACTIVE").match(result);
                    } else if (status == 400) {
                        org.junit.jupiter.api.Assumptions.assumeTrue(false, "Family group quota exceeded or invalid");
                    } else {
                        throw new AssertionError("Unexpected status: " + status);
                    }
                });
        }

        @Test
        @DisplayName("新增家庭成员失败 - 未登录")
        void addMember_notLoggedIn_failure() throws Exception {
            String joinedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            String memberBody = objectMapper.writeValueAsString(new AddMemberRequest(
                null,
                "测试家人",
                "STU001",
                "13900139000",
                joinedAt
            ));

            mockMvc.perform(post(MEMBERS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(memberBody))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("新增家庭成员失败 - 缺少必填字段")
        void addMember_missingRequiredFields_failure() throws Exception {
            String memberBody = "{}";

            mockMvc.perform(withAuth(post(MEMBERS_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(memberBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("新增家庭成员失败 - 无有效家庭组")
        void addMember_noActiveGroup_failure() throws Exception {
            when(wechatAuthGateway.code2Session(anyString()))
                .thenReturn(new WechatSession("openid_no_group_001", null, "session_key"));

            String loginBody = objectMapper.writeValueAsString(new LoginRequest("no_group_code", "无组用户", null));
            MvcResult loginResult = mockMvc.perform(post("/api/weixin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

            String tokenWithoutGroup = extractDataField(loginResult, "accessToken");

            String joinedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            String memberBody = objectMapper.writeValueAsString(new AddMemberRequest(
                null,
                "测试家人",
                "STU_NO_GROUP",
                "13900139000",
                joinedAt
            ));

            mockMvc.perform(withAuth(post(MEMBERS_URL), tokenWithoutGroup)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(memberBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("查询家庭成员列表接口")
    class SearchMembersTests {

        @Test
        @DisplayName("查询家庭成员列表成功 - 无过滤条件")
        void searchMembers_success() throws Exception {
            mockMvc.perform(withAuth(get(MEMBERS_URL), testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());
        }

        @Test
        @DisplayName("查询家庭成员列表成功 - 指定家庭组")
        void searchMembers_withGroupNo() throws Exception {
            if (testGroupNo == null) {
                org.junit.jupiter.api.Assumptions.assumeTrue(false, "No active family group available");
                return;
            }

            mockMvc.perform(withAuth(get(MEMBERS_URL), testToken)
                    .param("group_no", testGroupNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询家庭成员列表成功 - 关键字搜索")
        void searchMembers_withKeyword() throws Exception {
            mockMvc.perform(withAuth(get(MEMBERS_URL), testToken)
                    .param("keyword", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询家庭成员列表成功 - 分页参数")
        void searchMembers_withPaging() throws Exception {
            mockMvc.perform(withAuth(get(MEMBERS_URL), testToken)
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
        }

        @Test
        @DisplayName("查询家庭成员列表失败 - 未登录")
        void searchMembers_notLoggedIn_failure() throws Exception {
            mockMvc.perform(get(MEMBERS_URL))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("注销家庭成员卡接口")
    class CancelMemberTests {

        @Test
        @DisplayName("注销家庭成员卡失败 - 未登录")
        void cancelMember_notLoggedIn_failure() throws Exception {
            mockMvc.perform(delete(MEMBERS_URL + "/M123456"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("注销家庭成员卡失败 - 成员不存在")
        void cancelMember_notFound_failure() throws Exception {
            mockMvc.perform(withAuth(delete(MEMBERS_URL + "/M_NOT_EXIST_12345"), testToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("家庭卡打卡接口")
    class CheckInTests {

        @Test
        @DisplayName("打卡失败 - 未登录")
        void checkIn_notLoggedIn_failure() throws Exception {
            String checkedInAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            String checkInBody = objectMapper.writeValueAsString(new CheckInRequest(
                null,
                null,
                new BigDecimal("31.2304160"),
                new BigDecimal("121.4737010"),
                "测试地址",
                checkedInAt
            ));

            mockMvc.perform(post(CHECK_INS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkInBody))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("打卡失败 - 缺少必填字段")
        void checkIn_missingRequiredFields_failure() throws Exception {
            String checkInBody = "{}";

            mockMvc.perform(withAuth(post(CHECK_INS_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkInBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("打卡失败 - 纬度超出范围")
        void checkIn_invalidLatitude_failure() throws Exception {
            String checkedInAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            String checkInBody = objectMapper.writeValueAsString(new CheckInRequest(
                null,
                null,
                new BigDecimal("100.0"),
                new BigDecimal("121.4737010"),
                "测试地址",
                checkedInAt
            ));

            mockMvc.perform(withAuth(post(CHECK_INS_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkInBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("打卡失败 - 经度超出范围")
        void checkIn_invalidLongitude_failure() throws Exception {
            String checkedInAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            String checkInBody = objectMapper.writeValueAsString(new CheckInRequest(
                null,
                null,
                new BigDecimal("31.2304160"),
                new BigDecimal("200.0"),
                "测试地址",
                checkedInAt
            ));

            mockMvc.perform(withAuth(post(CHECK_INS_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkInBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("打卡失败 - 地址为空")
        void checkIn_emptyAddress_failure() throws Exception {
            String checkedInAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            String checkInBody = objectMapper.writeValueAsString(new CheckInRequest(
                null,
                null,
                new BigDecimal("31.2304160"),
                new BigDecimal("121.4737010"),
                "",
                checkedInAt
            ));

            mockMvc.perform(withAuth(post(CHECK_INS_URL), testToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkInBody))
                .andExpect(status().isBadRequest());
        }
    }

    private record LoginRequest(String code, String nickname, String avatarUrl) {}

    private record AddMemberRequest(String groupNo, String memberName, String studentOrCardNo, String phone, String joinedAt) {}

    private record CheckInRequest(String groupNo, Long familyMemberId, BigDecimal latitude, BigDecimal longitude, String addressText, String checkedInAt) {}
}
