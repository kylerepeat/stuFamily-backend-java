package com.stufamily.backend.weixinapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stufamily.backend.family.application.dto.FamilyCheckInView;
import com.stufamily.backend.family.application.dto.FamilyGroupQuotaView;
import com.stufamily.backend.family.application.dto.FamilyMemberView;
import com.stufamily.backend.family.application.service.FamilyApplicationService;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.security.SecurityUser;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WeixinFamilyControllerTest {

    private MockMvc mockMvc;
    private FamilyApplicationService familyApplicationService;

    @BeforeEach
    void setUp() {
        familyApplicationService = Mockito.mock(FamilyApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new WeixinFamilyController(familyApplicationService)).build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addShouldReturnMember() throws Exception {
        mockUser();
        when(familyApplicationService.addMember(any())).thenReturn(new FamilyMemberView("M1", "Tom", "S1",
            "13800000000", OffsetDateTime.parse("2026-03-24T10:00:00+08:00"), "ACTIVE",
            OffsetDateTime.parse("2026-12-31T23:59:59+08:00"), "https://example.com/avatar.png"));
        mockMvc.perform(post("/api/weixin/family/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberName\":\"Tom\",\"studentOrCardNo\":\"S1\",\"phone\":\"13800000000\","
                    + "\"joinedAt\":\"2026-03-24T10:00:00\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.memberNo").value("M1"));
    }

    @Test
    void addCheckInShouldReturnRecord() throws Exception {
        mockUser();
        when(familyApplicationService.addCheckIn(any())).thenReturn(new FamilyCheckInView(
            "CK001",
            "FG001",
            8L,
            1001L,
            new BigDecimal("31.2304160"),
            new BigDecimal("121.4737010"),
            "addr",
            OffsetDateTime.parse("2026-03-31T09:00:00+08:00")
        ));
        mockMvc.perform(post("/api/weixin/family/check-ins")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupNo\":\"FG001\",\"familyMemberId\":1001,\"latitude\":31.2304160,\"longitude\":121.4737010,"
                    + "\"addressText\":\"addr\",\"checkedInAt\":\"2026-03-31T09:00:00\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.checkinNo").value("CK001"))
            .andExpect(jsonPath("$.data.groupNo").value("FG001"))
            .andExpect(jsonPath("$.data.checkinUserId").value(8))
            .andExpect(jsonPath("$.data.familyMemberId").value(1001));
    }

    @Test
    void searchShouldReturnList() throws Exception {
        mockUser();
        when(familyApplicationService.searchMembers(8L, null, "Tom", 1, 10)).thenReturn(
            PageResult.of(List.of(new FamilyMemberView("M1", "Tom", "S1", "13800000000",
                OffsetDateTime.parse("2026-03-24T10:00:00+08:00"), "ACTIVE",
                OffsetDateTime.parse("2026-12-31T23:59:59+08:00"), "https://example.com/avatar.png")), 1, 1, 10));
        mockMvc.perform(get("/api/weixin/family/members")
                .param("keyword", "Tom")
                .param("page_no", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].memberName").value("Tom"));
    }

    @Test
    void cancelShouldReturnSuccess() throws Exception {
        mockUser();
        doNothing().when(familyApplicationService).cancelExpiredCard(eq(8L), eq("M1"));
        mockMvc.perform(delete("/api/weixin/family/members/M1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void groupQuotaShouldReturnSummary() throws Exception {
        mockUser();
        when(familyApplicationService.getCurrentGroupQuota(8L)).thenReturn(
            new FamilyGroupQuotaView(true, List.of(
                new FamilyGroupQuotaView.GroupQuotaView(
                    "FG0001",
                    "ACTIVE",
                    OffsetDateTime.parse("2026-12-31T23:59:59+08:00"),
                    5,
                    2,
                    3,
                    "FAMILY_CARD",
                    101L,
                    "Family Card",
                    1001L,
                    "SEMESTER",
                    6
                )
            ))
        );
        mockMvc.perform(get("/api/weixin/family/group/quota"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.hasActiveGroup").value(true))
            .andExpect(jsonPath("$.data.groups[0].maxMembers").value(5))
            .andExpect(jsonPath("$.data.groups[0].currentMembers").value(2))
            .andExpect(jsonPath("$.data.groups[0].durationType").value("SEMESTER"));
    }

    private void mockUser() {
        SecurityUser user = new SecurityUser(8L, "wx", "", List.of("WECHAT"));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}