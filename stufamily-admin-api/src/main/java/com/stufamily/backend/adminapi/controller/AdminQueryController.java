package com.stufamily.backend.adminapi.controller;

import com.stufamily.backend.adminquery.application.dto.AdminFamilyCardWithWechatUserView;
import com.stufamily.backend.adminquery.application.dto.AdminFamilyCheckInView;
import com.stufamily.backend.adminquery.application.dto.AdminFamilyMemberWithWechatUserView;
import com.stufamily.backend.adminquery.application.dto.AdminFilterOptionsView;
import com.stufamily.backend.adminquery.application.dto.AdminMonthlyIncomeStatsView;
import com.stufamily.backend.adminquery.application.dto.AdminOrderWithWechatUserView;
import com.stufamily.backend.adminquery.application.dto.AdminWechatUserView;
import com.stufamily.backend.adminquery.application.service.AdminQueryApplicationService;
import com.stufamily.backend.shared.api.ApiResponse;
import com.stufamily.backend.shared.api.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminQueryController {

    private final AdminQueryApplicationService adminQueryApplicationService;

    public AdminQueryController(AdminQueryApplicationService adminQueryApplicationService) {
        this.adminQueryApplicationService = adminQueryApplicationService;
    }

    @GetMapping("/filter-options")
    public ApiResponse<AdminFilterOptionsView> listFilterOptions() {
        return ApiResponse.success(adminQueryApplicationService.listFilterOptions());
    }

    @GetMapping("/weixin-users")
    public ApiResponse<PageResult<AdminWechatUserView>> listWechatUsers(
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(adminQueryApplicationService.listWechatUsers(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResult<AdminOrderWithWechatUserView>> listOrders(
        @RequestParam(name = "order_status", required = false) String orderStatus,
        @RequestParam(name = "order_type", required = false) String orderType,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(adminQueryApplicationService.listOrders(orderStatus, orderType, keyword, pageNo, pageSize));
    }

    @GetMapping("/family-cards")
    public ApiResponse<PageResult<AdminFamilyCardWithWechatUserView>> listFamilyCards(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(adminQueryApplicationService.listFamilyCards(status, keyword, pageNo, pageSize));
    }

    @GetMapping("/family-members")
    public ApiResponse<PageResult<AdminFamilyMemberWithWechatUserView>> listFamilyMembers(
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(adminQueryApplicationService.listFamilyMembers(keyword, pageNo, pageSize));
    }

    @GetMapping("/family-checkins")
    public ApiResponse<PageResult<AdminFamilyCheckInView>> listFamilyCheckIns(
        @RequestParam(name = "family_member_id", required = false) Long familyMemberId,
        @RequestParam(name = "wechat_user_id", required = false) Long wechatUserId,
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(
            adminQueryApplicationService.listFamilyCheckIns(familyMemberId, wechatUserId, pageNo, pageSize)
        );
    }

    @GetMapping("/orders/monthly-income-stats")
    public ApiResponse<AdminMonthlyIncomeStatsView> monthlyIncomeStats(
        @RequestParam(name = "start_month", required = false) String startMonth,
        @RequestParam(name = "end_month", required = false) String endMonth,
        @RequestParam(name = "product_type", required = false) String productType,
        @RequestParam(name = "product_id", required = false) Long productId) {
        return ApiResponse.success(
            adminQueryApplicationService.monthlyIncomeStats(startMonth, endMonth, productType, productId)
        );
    }
}
