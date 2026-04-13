package com.stufamily.backend.weixinapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.identity.application.service.AuthApplicationService;
import com.stufamily.backend.order.application.dto.OrderCreateResult;
import com.stufamily.backend.order.application.dto.PurchasedProductView;
import com.stufamily.backend.order.application.service.OrderApplicationService;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.security.SecurityUser;
import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateResponse;
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

class WeixinOrderControllerTest {

    private MockMvc mockMvc;
    private OrderApplicationService orderApplicationService;
    private AuthApplicationService authApplicationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        orderApplicationService = Mockito.mock(OrderApplicationService.class);
        authApplicationService = Mockito.mock(AuthApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
            new WeixinOrderController(orderApplicationService, authApplicationService)).build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShouldReturnOrder() throws Exception {
        SecurityUser user = new SecurityUser(7L, "wx", "", List.of("WECHAT"));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(authApplicationService.requireOpenid(7L)).thenReturn("openid");
        when(orderApplicationService.createOrder(any(), any())).thenReturn(
            new OrderCreateResult("ORD1", "PENDING_PAYMENT", 100L, new WechatPayCreateResponse("p", "n", "s", "t"))
        );

        mockMvc.perform(post("/api/weixin/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new Req("FAMILY_CARD", 1L, null, "YEAR", "2026-03-30", "Tom", "S1001", "13800138000", 100L))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderNo").value("ORD1"));
    }

    @Test
    void statusShouldReturnOrderStatus() throws Exception {
        when(orderApplicationService.findOrderStatus("ORDX")).thenReturn("PAID");
        mockMvc.perform(get("/api/weixin/orders/ORDX/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("PAID"));
    }

    @Test
    void listPurchasedProductsShouldReturnPage() throws Exception {
        SecurityUser user = new SecurityUser(7L, "wx", "", List.of("WECHAT"));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(orderApplicationService.listPurchasedProducts(7L, "FAMILY_CARD", 1, 20)).thenReturn(PageResult.of(
            List.of(new PurchasedProductView(
                "ORD-PAID-1",
                "FAMILY_CARD",
                "PAID",
                OffsetDateTime.parse("2026-03-26T10:00:00+08:00"),
                101L,
                "FAMILY_CARD",
                "Family Card",
                "Brief",
                "[\"https://example.com/p1.png\"]",
                "YEAR",
                12,
                OffsetDateTime.parse("2026-03-26T10:00:00+08:00"),
                OffsetDateTime.parse("2027-03-26T10:00:00+08:00"),
                19900L,
                1,
                19900L,
                5,
                "非常满意",
                OffsetDateTime.parse("2026-03-27T10:00:00+08:00")
            )),
            1,
            1,
            20
        ));

        mockMvc.perform(get("/api/weixin/orders/purchased-products")
                .param("product_type", "FAMILY_CARD")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].orderNo").value("ORD-PAID-1"))
            .andExpect(jsonPath("$.data.items[0].reviewStars").value(5))
            .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void submitReviewShouldReturnOk() throws Exception {
        SecurityUser user = new SecurityUser(7L, "wx", "", List.of("WECHAT"));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        mockMvc.perform(post("/api/weixin/orders/ORD-PAID-1/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "stars": 5,
                      "content": "服务很好"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"));

        verify(orderApplicationService).submitServiceReview(any());
    }

    @Test
    void listPurchasedProductsShouldQueryAllWhenProductTypeNotProvided() throws Exception {
        SecurityUser user = new SecurityUser(7L, "wx", "", List.of("WECHAT"));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(orderApplicationService.listPurchasedProducts(7L, null, 1, 20))
            .thenReturn(PageResult.of(List.of(), 0, 1, 20));

        mockMvc.perform(get("/api/weixin/orders/purchased-products")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0));

        verify(orderApplicationService).listPurchasedProducts(7L, null, 1, 20);
    }

    private record Req(String productType, Long productId, Long skuId, String durationType, String cardApplyDate,
                       String applicantName, String applicantStudentOrCardNo, String applicantPhone, Long amountCents) {
    }
}
