package com.stufamily.backend.adminapi.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stufamily.backend.adminquery.application.dto.AdminFamilyCardWithWechatUserView;
import com.stufamily.backend.adminquery.application.dto.AdminFamilyCheckInView;
import com.stufamily.backend.adminquery.application.dto.AdminFamilyMemberWithWechatUserView;
import com.stufamily.backend.adminquery.application.dto.AdminFilterOptionsView;
import com.stufamily.backend.adminquery.application.dto.AdminMonthlyAmountView;
import com.stufamily.backend.adminquery.application.dto.AdminMonthlyIncomeStatsView;
import com.stufamily.backend.adminquery.application.dto.AdminOrderWithWechatUserView;
import com.stufamily.backend.adminquery.application.dto.AdminSelectOptionView;
import com.stufamily.backend.adminquery.application.dto.AdminWechatUserView;
import com.stufamily.backend.adminquery.application.service.AdminQueryApplicationService;
import com.stufamily.backend.shared.api.PageResult;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminQueryControllerTest {

    private MockMvc mockMvc;
    private AdminQueryApplicationService adminQueryApplicationService;

    @BeforeEach
    void setUp() {
        adminQueryApplicationService = Mockito.mock(AdminQueryApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminQueryController(adminQueryApplicationService)).build();
    }

    @Test
    void shouldListFilterOptions() throws Exception {
        when(adminQueryApplicationService.listFilterOptions())
            .thenReturn(new AdminFilterOptionsView(
                List.of(new AdminSelectOptionView("ON_SHELF", "上架")),
                List.of(new AdminSelectOptionView("ACTIVE", "启用")),
                List.of(new AdminSelectOptionView("PAID", "已支付")),
                List.of(new AdminSelectOptionView("FAMILY_CARD", "家庭卡")),
                List.of(new AdminSelectOptionView("ACTIVE", "生效中"))
            ));

        mockMvc.perform(get("/api/admin/filter-options"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.productPublishStatuses[0].value").value("ON_SHELF"))
            .andExpect(jsonPath("$.data.orderTypes[0].value").value("FAMILY_CARD"));
    }

    @Test
    void shouldListWechatUsers() throws Exception {
        when(adminQueryApplicationService.listWechatUsers(eq("wx"), eq("ACTIVE"), eq(1), eq(20)))
            .thenReturn(PageResult.of(List.of(new AdminWechatUserView(1L, "U1", "WECHAT", "ACTIVE", "openid-1",
                "nick-1", "https://img", "138", null, null)), 1, 1, 20));

        mockMvc.perform(get("/api/admin/weixin-users")
                .param("keyword", "wx")
                .param("status", "ACTIVE")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].openid").value("openid-1"))
            .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldListOrdersWithWechatUser() throws Exception {
        when(adminQueryApplicationService.listOrders(eq("PAID"), eq("FAMILY_CARD"), eq("ORD"), eq(1), eq(10)))
            .thenReturn(PageResult.of(List.of(new AdminOrderWithWechatUserView(1L, "ORD1", 2L, "FAMILY_CARD", "PAID",
                19900L, "CNY", null, null, "openid-2", "nick-2", "https://img2")), 1, 1, 10));

        mockMvc.perform(get("/api/admin/orders")
                .param("order_status", "PAID")
                .param("order_type", "FAMILY_CARD")
                .param("keyword", "ORD")
                .param("page_no", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].buyerNickname").value("nick-2"));
    }

    @Test
    void shouldListFamilyCardsWithWechatUser() throws Exception {
        when(adminQueryApplicationService.listFamilyCards(eq("ACTIVE"), eq("G"), eq(1), eq(15)))
            .thenReturn(PageResult.of(List.of(new AdminFamilyCardWithWechatUserView(1L, "G1", 10L, 2L,
                3, 2, "ACTIVE", null, null, null, "openid-3", "nick-3", "https://img3")), 1, 1, 15));

        mockMvc.perform(get("/api/admin/family-cards")
                .param("status", "ACTIVE")
                .param("keyword", "G")
                .param("page_no", "1")
                .param("page_size", "15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].ownerOpenid").value("openid-3"));
    }

    @Test
    void shouldListFamilyMembers() throws Exception {
        when(adminQueryApplicationService.listFamilyMembers(eq("Tom"), eq(1), eq(10)))
            .thenReturn(PageResult.of(List.of(new AdminFamilyMemberWithWechatUserView(
                1L, "M1", "Tom", "S1", "13800000000", "ACTIVE", null, 99L, "G001", 2L, "owner-1"
            )), 1, 1, 10));

        mockMvc.perform(get("/api/admin/family-members")
                .param("keyword", "Tom")
                .param("page_no", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].memberName").value("Tom"))
            .andExpect(jsonPath("$.data.items[0].groupNo").value("G001"));
    }

    @Test
    void shouldListFamilyCheckIns() throws Exception {
        when(adminQueryApplicationService.listFamilyCheckIns(eq(1001L), eq(8L), eq(1), eq(10)))
            .thenReturn(PageResult.of(List.of(new AdminFamilyCheckInView(
                1L,
                "CK001",
                99L,
                "FG001",
                8L,
                "openid-8",
                "nick-8",
                1001L,
                "M001",
                "Tom",
                new BigDecimal("31.2304160"),
                new BigDecimal("121.4737010"),
                "addr",
                OffsetDateTime.parse("2026-03-31T09:00:00+08:00"),
                OffsetDateTime.parse("2026-03-31T09:00:05+08:00")
            )), 1, 1, 10));

        mockMvc.perform(get("/api/admin/family-checkins")
                .param("family_member_id", "1001")
                .param("wechat_user_id", "8")
                .param("page_no", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].checkinNo").value("CK001"))
            .andExpect(jsonPath("$.data.items[0].familyMemberName").value("Tom"))
            .andExpect(jsonPath("$.data.items[0].ownerNickname").value("nick-8"));
    }

    @Test
    void shouldReturnMonthlyIncomeStats() throws Exception {
        when(adminQueryApplicationService.monthlyIncomeStats(eq("2026-01"), eq("2026-03"), eq("FAMILY_CARD"), eq(1L)))
            .thenReturn(new AdminMonthlyIncomeStatsView(
                List.of(new AdminMonthlyAmountView("2026-01", 10000L)),
                List.of(new AdminMonthlyAmountView("2026-01", 1000L)),
                10000L,
                1000L,
                9000L
            ));

        mockMvc.perform(get("/api/admin/orders/monthly-income-stats")
                .param("start_month", "2026-01")
                .param("end_month", "2026-03")
                .param("product_type", "FAMILY_CARD")
                .param("product_id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.monthlyTotalIncome[0].month").value("2026-01"))
            .andExpect(jsonPath("$.data.totalRefundCents").value(1000))
            .andExpect(jsonPath("$.data.netIncomeCents").value(9000));
    }
}
