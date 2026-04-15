package com.stufamily.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeixinFamilyIntegrationTest extends IntegrationTestBase {

    @Test
    void addMember_shouldFail_whenNoActiveGroup() throws Exception {
        mockMvc.perform(post("/api/weixin/family/members")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "memberName", "李四",
                    "studentOrCardNo", "20260002",
                    "phone", "13800138002",
                    "joinedAt", "2026-04-15T10:00:00"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void addMember_shouldFail_whenMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/weixin/family/members")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "memberName", "李四"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void addMember_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/weixin/family/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "memberName", "李四",
                    "studentOrCardNo", "20260002",
                    "phone", "13800138002",
                    "joinedAt", "2026-04-15T10:00:00"
                ))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void addCheckIn_shouldFail_whenNoActiveGroup() throws Exception {
        mockMvc.perform(post("/api/weixin/family/check-ins")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "latitude", 31.230416,
                    "longitude", 121.473701,
                    "addressText", "测试地址",
                    "checkedInAt", "2026-04-15T10:00:00"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void addCheckIn_shouldFail_whenInvalidLatitude() throws Exception {
        mockMvc.perform(post("/api/weixin/family/check-ins")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "latitude", 91.0,
                    "longitude", 121.473701,
                    "addressText", "测试地址",
                    "checkedInAt", "2026-04-15T10:00:00"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void addCheckIn_shouldFail_whenInvalidLongitude() throws Exception {
        mockMvc.perform(post("/api/weixin/family/check-ins")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "latitude", 31.230416,
                    "longitude", 181.0,
                    "addressText", "测试地址",
                    "checkedInAt", "2026-04-15T10:00:00"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void addCheckIn_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/weixin/family/check-ins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "latitude", 31.230416,
                    "longitude", 121.473701,
                    "addressText", "测试地址",
                    "checkedInAt", "2026-04-15T10:00:00"
                ))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void searchMembers_shouldReturnEmptyList_whenNoMembers() throws Exception {
        mockMvc.perform(get("/api/weixin/family/members")
                .header("Authorization", "Bearer " + wechatAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void searchMembers_shouldSupportPagination() throws Exception {
        mockMvc.perform(get("/api/weixin/family/members")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .param("page_no", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.pageNo").value(1))
            .andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test
    void searchMembers_shouldSupportKeywordSearch() throws Exception {
        mockMvc.perform(get("/api/weixin/family/members")
                .header("Authorization", "Bearer " + wechatAccessToken)
                .param("keyword", "张三"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void searchMembers_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/family/members"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelMember_shouldFail_whenMemberNotFound() throws Exception {
        mockMvc.perform(delete("/api/weixin/family/members/{memberNo}", "M_NONEXISTENT")
                .header("Authorization", "Bearer " + wechatAccessToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void cancelMember_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/weixin/family/members/{memberNo}", "M123"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void groupQuota_shouldReturnNoActiveGroup_whenUserHasNoGroup() throws Exception {
        mockMvc.perform(get("/api/weixin/family/group/quota")
                .header("Authorization", "Bearer " + wechatAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.hasActiveGroup").value(false))
            .andExpect(jsonPath("$.data.groups").isArray());
    }

    @Test
    void groupQuota_shouldFail_whenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/weixin/family/group/quota"))
            .andExpect(status().isUnauthorized());
    }
}
