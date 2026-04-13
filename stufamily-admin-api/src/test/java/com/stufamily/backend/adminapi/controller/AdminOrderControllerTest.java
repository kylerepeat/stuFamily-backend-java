package com.stufamily.backend.adminapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stufamily.backend.order.application.dto.AdminDisableFamilyGroupResult;
import com.stufamily.backend.order.application.dto.AdminProductReviewView;
import com.stufamily.backend.order.application.dto.AdminRefundResult;
import com.stufamily.backend.order.application.dto.AdminRefundView;
import com.stufamily.backend.order.application.service.AdminOrderApplicationService;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.exception.GlobalExceptionHandler;
import com.stufamily.backend.shared.security.SecurityUser;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminOrderControllerTest {

    private MockMvc mockMvc;
    private AdminOrderApplicationService adminOrderApplicationService;

    @BeforeEach
    void setUp() {
        adminOrderApplicationService = Mockito.mock(AdminOrderApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOrderController(adminOrderApplicationService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        SecurityUser principal = new SecurityUser(99L, "admin", "pwd", List.of("ADMIN"));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRefundOrder() throws Exception {
        when(adminOrderApplicationService.refundOrder(any())).thenReturn(
            new AdminRefundResult("ORD1001", "RFD1001", "SUCCESS", "WXRF1001",
                1000L, 1000L, 0L, "REFUNDED", "REFUNDED", OffsetDateTime.now())
        );

        mockMvc.perform(post("/api/admin/orders/ORD1001/refund")
                .contentType("application/json")
                .content("""
                    {
                      "refundAmountCents": 1000,
                      "reason": "user request"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.refundNo").value("RFD1001"))
            .andExpect(jsonPath("$.data.orderStatus").value("REFUNDED"));
    }

    @Test
    void shouldListRefunds() throws Exception {
        when(adminOrderApplicationService.listOrderRefunds("ORD2001", 1, 20)).thenReturn(PageResult.of(List.of(
            new AdminRefundView("RFD2001", "SUCCESS", "WXRF2001", 500L, "ops", OffsetDateTime.now(), OffsetDateTime.now())), 1, 1, 20));

        mockMvc.perform(get("/api/admin/orders/ORD2001/refunds")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].refundNo").value("RFD2001"))
            .andExpect(jsonPath("$.data.items[0].refundAmountCents").value(500));
    }

    @Test
    void shouldDisableFamilyGroup() throws Exception {
        when(adminOrderApplicationService.disableFamilyGroup(any())).thenReturn(
            new AdminDisableFamilyGroupResult("ORD3001", "FG3001", "CLOSED", 3, 2, OffsetDateTime.now())
        );

        mockMvc.perform(post("/api/admin/orders/ORD3001/disable-family-group"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderNo").value("ORD3001"))
            .andExpect(jsonPath("$.data.groupStatus").value("CLOSED"))
            .andExpect(jsonPath("$.data.disabledMemberCount").value(2));
    }

    @Test
    void shouldGetProductReviewByOrderId() throws Exception {
        when(adminOrderApplicationService.getProductReviewByOrderId(1L)).thenReturn(
            new AdminProductReviewView(
                11L,
                1L,
                "ORD1001",
                100L,
                200L,
                "FAMILY_CARD",
                5,
                "很好",
                OffsetDateTime.now(),
                OffsetDateTime.now()
            )
        );

        mockMvc.perform(get("/api/admin/orders/1/product-review"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reviewId").value(11))
            .andExpect(jsonPath("$.data.orderId").value(1))
            .andExpect(jsonPath("$.data.stars").value(5));
    }
}
