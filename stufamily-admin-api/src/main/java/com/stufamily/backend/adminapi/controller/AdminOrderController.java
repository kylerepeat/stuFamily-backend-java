package com.stufamily.backend.adminapi.controller;

import com.stufamily.backend.adminapi.request.AdminOrderRefundRequest;
import com.stufamily.backend.order.application.command.AdminDisableFamilyGroupCommand;
import com.stufamily.backend.order.application.command.AdminRefundCommand;
import com.stufamily.backend.order.application.dto.AdminDisableFamilyGroupResult;
import com.stufamily.backend.order.application.dto.AdminProductReviewView;
import com.stufamily.backend.order.application.dto.AdminRefundResult;
import com.stufamily.backend.order.application.dto.AdminRefundView;
import com.stufamily.backend.order.application.service.AdminOrderApplicationService;
import com.stufamily.backend.shared.api.ApiResponse;
import com.stufamily.backend.shared.api.PageResult;
import com.stufamily.backend.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderApplicationService adminOrderApplicationService;

    public AdminOrderController(AdminOrderApplicationService adminOrderApplicationService) {
        this.adminOrderApplicationService = adminOrderApplicationService;
    }

    @PostMapping("/{orderNo}/refund")
    public ApiResponse<AdminRefundResult> refundOrder(
        @PathVariable("orderNo") String orderNo,
        @Valid @RequestBody AdminOrderRefundRequest request) {
        Long operatorUserId = CurrentUser.requireUserId();
        return ApiResponse.success(adminOrderApplicationService.refundOrder(
            new AdminRefundCommand(orderNo, request.refundAmountCents(), request.reason(), operatorUserId)
        ));
    }

    @GetMapping("/{orderNo}/refunds")
    public ApiResponse<PageResult<AdminRefundView>> listRefunds(
        @PathVariable("orderNo") String orderNo,
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(adminOrderApplicationService.listOrderRefunds(orderNo, pageNo, pageSize));
    }

    @GetMapping("/{orderId}/product-review")
    public ApiResponse<AdminProductReviewView> getProductReviewByOrderId(@PathVariable("orderId") Long orderId) {
        return ApiResponse.success(adminOrderApplicationService.getProductReviewByOrderId(orderId));
    }

    @PostMapping("/{orderNo}/disable-family-group")
    public ApiResponse<AdminDisableFamilyGroupResult> disableFamilyGroup(@PathVariable("orderNo") String orderNo) {
        Long operatorUserId = CurrentUser.requireUserId();
        return ApiResponse.success(adminOrderApplicationService.disableFamilyGroup(
            new AdminDisableFamilyGroupCommand(orderNo, operatorUserId)
        ));
    }
}
