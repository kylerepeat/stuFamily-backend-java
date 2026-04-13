package com.stufamily.backend.weixinapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.home.application.dto.HomePageView;
import com.stufamily.backend.home.application.dto.HomeProductDetailView;
import com.stufamily.backend.home.application.dto.HomeProductView;
import com.stufamily.backend.home.application.dto.ParentMessageReplyView;
import com.stufamily.backend.home.application.dto.ParentMessageView;
import com.stufamily.backend.home.application.service.HomeApplicationService;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import com.stufamily.backend.shared.exception.GlobalExceptionHandler;
import com.stufamily.backend.shared.security.SecurityUser;
import java.math.BigDecimal;
import java.time.LocalDate;
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

class WeixinHomeControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private HomeApplicationService homeApplicationService;

    @BeforeEach
    void setUp() {
        homeApplicationService = Mockito.mock(HomeApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new WeixinHomeController(homeApplicationService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnHome() throws Exception {
        when(homeApplicationService.loadHomePage()).thenReturn(new HomePageView(
            List.of(new HomePageView.BannerView(1L, "t", "i")),
            "welcome",
            new HomePageView.SiteProfileView("c", "i", "张老师", "p", "wx-service", "https://example.com/wx-qr.png", "a",
                new BigDecimal("31.2304160"), new BigDecimal("121.4737010")),
            List.of(new HomePageView.NoticeView("系统通知", ""))
        ));
        mockMvc.perform(get("/api/weixin/home/index"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.banners[0].title").value("t"))
            .andExpect(jsonPath("$.data.siteProfile.contactPerson").value("张老师"))
            .andExpect(jsonPath("$.data.siteProfile.contactWechatQrUrl").value("https://example.com/wx-qr.png"))
            .andExpect(jsonPath("$.data.bannerSlogan").value("welcome"))
            .andExpect(jsonPath("$.data.notices[0].title").value("系统通知"));
    }

    @Test
    void shouldReturnProducts() throws Exception {
        when(homeApplicationService.loadProducts(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(new HomeProductView(1L, "FAMILY_CARD", "Family Card", 19900L, true, "ON_SHELF")));

        mockMvc.perform(get("/api/weixin/home/products")
                .param("sale_start_at", "2026-03-01")
                .param("sale_end_at", "2026-03-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].type").value("FAMILY_CARD"));
    }

    @Test
    void shouldRequireProductDateParams() throws Exception {
        when(homeApplicationService.loadProducts(isNull(), isNull()))
            .thenThrow(new BusinessException(ErrorCode.INVALID_PARAM, "sale_start_at and sale_end_at are required"));
        mockMvc.perform(get("/api/weixin/home/products"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void shouldReturnProductDetail() throws Exception {
        when(homeApplicationService.loadProductDetail(1L)).thenReturn(new HomeProductDetailView(
            1L,
            "P1",
            "FAMILY_CARD",
            "Family Card",
            "sub",
            "detail",
            List.of("https://example.com/a.png"),
            "desk",
            "138",
            null,
            null,
            null,
            null,
            "ON_SHELF",
            false,
            true,
            100,
            1L,
            2L,
            3L,
            List.of(new HomeProductDetailView.FamilyCardPlanView(10L, "MONTH", 1, 19900L, 3, true)),
            List.of()
        ));

        mockMvc.perform(get("/api/weixin/home/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.familyCardPlans[0].durationType").value("MONTH"));
    }

    @Test
    void shouldCreateMessage() throws Exception {
        mockUser();
        when(homeApplicationService.createParentMessage(any())).thenReturn(
            new ParentMessageView(1L, "ParentA", "https://example.com/a.png", "message", OffsetDateTime.now(), List.of())
        );

        mockMvc.perform(post("/api/weixin/home/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new MessageRequest("message"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").value("message"));
    }

    @Test
    void shouldListMyMessages() throws Exception {
        mockUser();
        when(homeApplicationService.listMyParentMessages(8L, 1, 10)).thenReturn(PageResult.of(
            List.of(new ParentMessageView(
                1L,
                "ParentA",
                null,
                "my message",
                OffsetDateTime.now(),
                List.of(new ParentMessageReplyView(2L, "ADMIN", "ADMIN", null, "admin reply", OffsetDateTime.now()))
            )),
            1,
            1,
            10
        ));

        mockMvc.perform(get("/api/weixin/home/messages/mine")
                .param("page_no", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].content").value("my message"))
            .andExpect(jsonPath("$.data.items[0].replies[0].senderType").value("ADMIN"))
            .andExpect(jsonPath("$.data.total").value(1));
    }

    private void mockUser() {
        SecurityUser user = new SecurityUser(8L, "wx", "", List.of("WECHAT"));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private record MessageRequest(String content) {
    }
}
