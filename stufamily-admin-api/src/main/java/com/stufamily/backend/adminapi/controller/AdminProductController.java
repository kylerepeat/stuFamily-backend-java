package com.stufamily.backend.adminapi.controller;

import com.stufamily.backend.adminapi.request.AdminProductUpdateRequest;
import com.stufamily.backend.home.application.command.AdminProductUpdateCommand;
import com.stufamily.backend.home.application.dto.AdminProductDetailView;
import com.stufamily.backend.home.application.dto.HomeProductView;
import com.stufamily.backend.home.application.service.HomeApplicationService;
import com.stufamily.backend.shared.api.ApiResponse;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final HomeApplicationService homeApplicationService;

    public AdminProductController(HomeApplicationService homeApplicationService) {
        this.homeApplicationService = homeApplicationService;
    }

    @GetMapping
    public ApiResponse<PageResult<HomeProductView>> list(
        @RequestParam(name = "sale_start_at", required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate saleStartAt,
        @RequestParam(name = "sale_end_at", required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate saleEndAt,
        @RequestParam(name = "publish_status", required = false) String publishStatus,
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(homeApplicationService.loadAdminProducts(
            saleStartAt, saleEndAt, publishStatus, pageNo, pageSize));
    }

    @GetMapping("/{productId}")
    public ApiResponse<AdminProductDetailView> getDetail(@PathVariable("productId") Long productId) {
        return ApiResponse.success(homeApplicationService.getAdminProductDetail(productId));
    }

    @PostMapping
    public ApiResponse<AdminProductDetailView> create(@Valid @RequestBody AdminProductUpdateRequest request) {
        Long operatorUserId = CurrentUser.requireUserId();
        return ApiResponse.success(homeApplicationService.createAdminProduct(toCommand(request), operatorUserId));
    }

    @PutMapping("/{productId}")
    public ApiResponse<AdminProductDetailView> update(
        @PathVariable("productId") Long productId,
        @Valid @RequestBody AdminProductUpdateRequest request) {
        Long operatorUserId = CurrentUser.requireUserId();
        return ApiResponse.success(homeApplicationService.updateAdminProduct(productId, toCommand(request), operatorUserId));
    }

    @PostMapping("/{productId}/on-shelf")
    public ApiResponse<AdminProductDetailView> onShelf(@PathVariable("productId") Long productId) {
        Long operatorUserId = CurrentUser.requireUserId();
        return ApiResponse.success(homeApplicationService.onShelfAdminProduct(productId, operatorUserId));
    }

    @PostMapping("/{productId}/off-shelf")
    public ApiResponse<AdminProductDetailView> offShelf(@PathVariable("productId") Long productId) {
        Long operatorUserId = CurrentUser.requireUserId();
        return ApiResponse.success(homeApplicationService.offShelfAdminProduct(productId, operatorUserId));
    }

    private AdminProductUpdateCommand toCommand(AdminProductUpdateRequest request) {
        List<AdminProductUpdateCommand.FamilyCardPlanCommand> planCommands = null;
        if (request.familyCardPlans() != null) {
            planCommands = request.familyCardPlans().stream()
                .map(plan -> new AdminProductUpdateCommand.FamilyCardPlanCommand(
                    plan.id(),
                    plan.durationType(),
                    plan.durationMonths(),
                    plan.priceCents(),
                    plan.maxFamilyMembers(),
                    plan.enabled()
                ))
                .toList();
        }
        List<AdminProductUpdateCommand.ValueAddedSkuCommand> skuCommands = null;
        if (request.valueAddedSkus() != null) {
            skuCommands = request.valueAddedSkus().stream()
                .map(sku -> new AdminProductUpdateCommand.ValueAddedSkuCommand(
                    sku.id(),
                    sku.title(),
                    sku.priceCents(),
                    sku.enabled()
                ))
                .toList();
        }
        return new AdminProductUpdateCommand(
            request.productType(),
            request.title(),
            request.subtitle(),
            request.detailContent(),
            request.imageUrls(),
            request.contactName(),
            request.contactPhone(),
            request.serviceStartAt(),
            request.serviceEndAt(),
            request.saleStartAt(),
            request.saleEndAt(),
            request.publishStatus(),
            request.top(),
            request.displayPriority(),
            planCommands,
            skuCommands
        );
    }
}
