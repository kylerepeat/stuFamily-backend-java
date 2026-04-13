package com.stufamily.backend.adminapi.controller;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.home.application.dto.AdminProductDetailView;
import com.stufamily.backend.home.application.dto.HomeProductView;
import com.stufamily.backend.home.application.service.HomeApplicationService;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import com.stufamily.backend.shared.exception.GlobalExceptionHandler;
import com.stufamily.backend.shared.security.SecurityUser;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminProductControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private HomeApplicationService homeApplicationService;

    @BeforeEach
    void setUp() {
        homeApplicationService = Mockito.mock(HomeApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminProductController(homeApplicationService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        SecurityUser principal = new SecurityUser(1L, "admin", "pwd", List.of("ADMIN"));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnProducts() throws Exception {
        when(homeApplicationService.loadAdminProducts(
            nullable(LocalDate.class), nullable(LocalDate.class), Mockito.eq("ON_SHELF"), Mockito.eq(1), Mockito.eq(20)))
            .thenReturn(PageResult.of(List.of(new HomeProductView(1L, "FAMILY_CARD", "title", 100L, true, "ON_SHELF")), 1, 1, 20));

        mockMvc.perform(get("/api/admin/products")
                .param("sale_start_at", "2026-03-01")
                .param("sale_end_at", "2026-03-31")
                .param("publish_status", "ON_SHELF")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].type").value("FAMILY_CARD"))
            .andExpect(jsonPath("$.data.items[0].publishStatus").value("ON_SHELF"));
    }

    @Test
    void shouldRequireDateParams() throws Exception {
        when(homeApplicationService.loadAdminProducts(isNull(), isNull(), isNull(), Mockito.isNull(), Mockito.isNull()))
            .thenThrow(new BusinessException(ErrorCode.INVALID_PARAM, "sale_start_at and sale_end_at are required"));
        mockMvc.perform(get("/api/admin/products"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
    }

    @Test
    void shouldCreateProduct() throws Exception {
        when(homeApplicationService.createAdminProduct(Mockito.any(), Mockito.eq(1L)))
            .thenReturn(new AdminProductDetailView(10L, "P10", "FAMILY_CARD", "title", "sub", "detail", List.of(),
                "name", "138", null, null, null, null, "DRAFT", false, true, 10, null, null, null,
                List.of(), List.of()));

        mockMvc.perform(post("/api/admin/products")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new ProductReq(
                    "FAMILY_CARD", "title", "sub", "detail", List.of(), "name", "138",
                    null, null, null, null, "DRAFT", true, 10, List.of(), List.of()
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        when(homeApplicationService.updateAdminProduct(Mockito.eq(10L), Mockito.any(), Mockito.eq(1L)))
            .thenReturn(new AdminProductDetailView(10L, "P10", "FAMILY_CARD", "new title", "sub", "detail", List.of(),
                "name", "138", null, null, null, null, "DRAFT", false, true, 10, null, null, null,
                List.of(), List.of()));

        mockMvc.perform(put("/api/admin/products/10")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new ProductReq(
                    "FAMILY_CARD", "new title", "sub", "detail", List.of(), "name", "138",
                    null, null, null, null, "DRAFT", true, 10, List.of(), List.of()
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("new title"));
    }

    @Test
    void shouldOnShelf() throws Exception {
        when(homeApplicationService.onShelfAdminProduct(10L, 1L))
            .thenReturn(new AdminProductDetailView(10L, "P10", "FAMILY_CARD", "title", "sub", "detail", List.of(),
                "name", "138", null, null, null, null, "ON_SHELF", false, true, 10, null, null, null,
                List.of(), List.of()));
        mockMvc.perform(post("/api/admin/products/10/on-shelf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.publishStatus").value("ON_SHELF"));
    }

    @Test
    void shouldOffShelf() throws Exception {
        when(homeApplicationService.offShelfAdminProduct(10L, 1L))
            .thenReturn(new AdminProductDetailView(10L, "P10", "FAMILY_CARD", "title", "sub", "detail", List.of(),
                "name", "138", null, null, null, null, "OFF_SHELF", false, true, 10, null, null, null,
                List.of(), List.of()));
        mockMvc.perform(post("/api/admin/products/10/off-shelf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.publishStatus").value("OFF_SHELF"));
    }

    private record ProductReq(
        String productType,
        String title,
        String subtitle,
        String detailContent,
        List<String> imageUrls,
        String contactName,
        String contactPhone,
        Object serviceStartAt,
        Object serviceEndAt,
        Object saleStartAt,
        Object saleEndAt,
        String publishStatus,
        Boolean top,
        Integer displayPriority,
        List<Object> familyCardPlans,
        List<Object> valueAddedSkus
    ) {
    }
}
