package com.stufamily.backend.adminapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.home.application.dto.AdminHomeNoticeView;
import com.stufamily.backend.home.application.dto.AdminHomepageBannerView;
import com.stufamily.backend.home.application.dto.AdminParentMessageDetailView;
import com.stufamily.backend.home.application.dto.AdminParentMessageNodeView;
import com.stufamily.backend.home.application.dto.AdminParentMessageView;
import com.stufamily.backend.home.application.dto.AdminSiteProfileView;
import com.stufamily.backend.home.application.service.HomeApplicationService;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.security.SecurityUser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminHomeControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private HomeApplicationService homeApplicationService;

    @BeforeEach
    void setUp() {
        homeApplicationService = Mockito.mock(HomeApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminHomeController(homeApplicationService)).build();
        SecurityUser principal = new SecurityUser(1L, "admin", "pwd", List.of("ADMIN"));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldListBanners() throws Exception {
        when(homeApplicationService.listAdminBanners(1, 20))
            .thenReturn(PageResult.of(List.of(new AdminHomepageBannerView(1L, "banner", "https://img", "NONE", null,
                1, true, null, null)), 1, 1, 20));

        mockMvc.perform(get("/api/admin/home/banners")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].title").value("banner"));
    }

    @Test
    void shouldUpdateBanner() throws Exception {
        when(homeApplicationService.updateAdminBanner(eq(1L), any(), eq(1L)))
            .thenReturn(new AdminHomepageBannerView(1L, "new banner", "https://img2", "NONE", null,
                9, true, null, null));

        mockMvc.perform(put("/api/admin/home/banners/1")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new BannerRequest("new banner", "https://img2",
                    "NONE", null, 9, true, null, null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sortOrder").value(9));
    }

    @Test
    void shouldDeleteBanner() throws Exception {
        doNothing().when(homeApplicationService).deleteAdminBanner(1L);

        mockMvc.perform(delete("/api/admin/home/banners/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));

        verify(homeApplicationService).deleteAdminBanner(1L);
    }

    @Test
    void shouldListNotices() throws Exception {
        when(homeApplicationService.listAdminNotices(1, 20))
            .thenReturn(PageResult.of(List.of(
                new AdminHomeNoticeView(1L, "系统通知", "内容", true, 1, null, null, null, null)
            ), 1, 1, 20));

        mockMvc.perform(get("/api/admin/home/notices")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].title").value("系统通知"));
    }

    @Test
    void shouldCreateNotice() throws Exception {
        when(homeApplicationService.createAdminNotice(any(), eq(1L)))
            .thenReturn(new AdminHomeNoticeView(1L, "系统通知", null, true, 10, null, null, null, null));

        mockMvc.perform(post("/api/admin/home/notices")
                .contentType("application/json")
                .content("""
                    {
                      "title":"系统通知",
                      "content":"",
                      "enabled":true,
                      "sortOrder":10
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("系统通知"));
    }

    @Test
    void shouldDeleteNotice() throws Exception {
        doNothing().when(homeApplicationService).deleteAdminNotice(1L);

        mockMvc.perform(delete("/api/admin/home/notices/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));

        verify(homeApplicationService).deleteAdminNotice(1L);
    }

    @Test
    void shouldListSiteProfiles() throws Exception {
        when(homeApplicationService.listAdminSiteProfiles(1, 20))
            .thenReturn(PageResult.of(List.of(new AdminSiteProfileView(1L, "community", "welcome", "intro", "王老师",
                "138", "wx-service", "https://example.com/wx-qr-1.png", "addr",
                new BigDecimal("31.2304160"), new BigDecimal("121.4737010"), true)), 1, 1, 20));

        mockMvc.perform(get("/api/admin/home/site-profiles")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].communityName").value("community"));
    }

    @Test
    void shouldGetSiteProfile() throws Exception {
        when(homeApplicationService.getAdminSiteProfile(1L))
            .thenReturn(new AdminSiteProfileView(1L, "community", "welcome", "intro", "王老师",
                "138", "wx-service", "https://example.com/wx-qr-1.png", "addr",
                new BigDecimal("31.2304160"), new BigDecimal("121.4737010"), true));

        mockMvc.perform(get("/api/admin/home/site-profiles/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.bannerSlogan").value("welcome"))
            .andExpect(jsonPath("$.data.contactPerson").value("王老师"));
    }

    @Test
    void shouldUpdateSiteProfile() throws Exception {
        when(homeApplicationService.updateAdminSiteProfile(eq(1L), any(), eq(1L)))
            .thenReturn(new AdminSiteProfileView(1L, "community2", "welcome2", "intro2", "李老师",
                "139", "wx2", "https://example.com/wx-qr-2.png", "addr2",
                new BigDecimal("30.1234567"), new BigDecimal("120.1234567"), true));

        mockMvc.perform(put("/api/admin/home/site-profiles/1")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new SiteRequest("community2", "welcome2",
                    "intro2", "李老师", "139", "wx2", "https://example.com/wx-qr-2.png", "addr2",
                    new BigDecimal("30.1234567"), new BigDecimal("120.1234567"), true))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.contactWechat").value("wx2"));
    }

    @Test
    void shouldDeleteSiteProfile() throws Exception {
        doNothing().when(homeApplicationService).deleteAdminSiteProfile(1L);

        mockMvc.perform(delete("/api/admin/home/site-profiles/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));

        verify(homeApplicationService).deleteAdminSiteProfile(1L);
    }

    @Test
    void shouldListMessages() throws Exception {
        when(homeApplicationService.listAdminParentMessages(false, false, 1, 20))
            .thenReturn(PageResult.of(List.of(
                new AdminParentMessageView(1L, 8L, "家长A", null, "留言", false, false, false, null, null, null)
            ), 1, 1, 20));

        mockMvc.perform(get("/api/admin/home/messages")
                .param("viewed", "false")
                .param("replied", "false")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].content").value("留言"));
    }

    @Test
    void shouldGetMessageDetail() throws Exception {
        when(homeApplicationService.getAdminParentMessageDetail(1L))
            .thenReturn(new AdminParentMessageDetailView(
                new AdminParentMessageView(1L, 8L, "家长A", null, "留言", true, false, false, null, null, null),
                List.of(new AdminParentMessageNodeView(1L, null, 1L, 8L, "USER", "家长A", null, "留言", null))
            ));

        mockMvc.perform(get("/api/admin/home/messages/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.root.id").value(1))
            .andExpect(jsonPath("$.data.nodes[0].senderType").value("USER"));
    }

    @Test
    void shouldReplyMessage() throws Exception {
        when(homeApplicationService.replyAdminParentMessage(eq(1L), any(), eq(1L)))
            .thenReturn(new AdminParentMessageDetailView(
                new AdminParentMessageView(1L, 8L, "家长A", null, "留言", true, true, false, null, null, null),
                List.of(new AdminParentMessageNodeView(2L, 1L, 1L, 8L, "ADMIN", "管理员", null, "收到", null))
            ));

        mockMvc.perform(post("/api/admin/home/messages/1/reply")
                .contentType("application/json")
                .content("{\"content\":\"收到\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nodes[0].senderType").value("ADMIN"));
    }

    @Test
    void shouldCloseMessage() throws Exception {
        doNothing().when(homeApplicationService).closeAdminParentMessage(1L, 1L);
        mockMvc.perform(post("/api/admin/home/messages/1/close"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void shouldDeleteMessage() throws Exception {
        doNothing().when(homeApplicationService).deleteAdminParentMessage(1L, 1L);
        mockMvc.perform(delete("/api/admin/home/messages/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));
    }

    private record BannerRequest(
        String title,
        String imageUrl,
        String linkType,
        String linkTarget,
        Integer sortOrder,
        Boolean enabled,
        String startAt,
        String endAt
    ) {
    }

    private record SiteRequest(
        String communityName,
        String bannerSlogan,
        String introText,
        String contactPerson,
        String contactPhone,
        String contactWechat,
        String contactWechatQrUrl,
        String addressText,
        BigDecimal latitude,
        BigDecimal longitude,
        Boolean active
    ) {
    }
}
