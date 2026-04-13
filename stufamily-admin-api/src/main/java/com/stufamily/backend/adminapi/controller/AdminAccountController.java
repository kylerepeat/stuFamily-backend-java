package com.stufamily.backend.adminapi.controller;

import com.stufamily.backend.adminapi.request.AdminChangePasswordRequest;
import com.stufamily.backend.adminapi.request.AdminCreateAccountRequest;
import com.stufamily.backend.adminapi.request.AdminPasswordStrengthRequest;
import com.stufamily.backend.identity.application.command.ChangeAdminPasswordCommand;
import com.stufamily.backend.identity.application.command.CreateAdminAccountCommand;
import com.stufamily.backend.identity.application.dto.AdminAccountView;
import com.stufamily.backend.identity.application.service.AdminAccountApplicationService;
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
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final AdminAccountApplicationService adminAccountApplicationService;

    public AdminAccountController(AdminAccountApplicationService adminAccountApplicationService) {
        this.adminAccountApplicationService = adminAccountApplicationService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminAccountView>> list(
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "page_no", required = false) Integer pageNo,
        @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(adminAccountApplicationService.listAdminAccounts(keyword, status, pageNo, pageSize));
    }

    @PostMapping
    public ApiResponse<AdminAccountView> create(@Valid @RequestBody AdminCreateAccountRequest request) {
        return ApiResponse.success(adminAccountApplicationService.createAdminAccount(
            new CreateAdminAccountCommand(
                request.username(),
                request.password(),
                request.nickname(),
                request.phone(),
                request.email()
            )));
    }

    @PostMapping("/{userId}/disable")
    public ApiResponse<Void> disable(@PathVariable("userId") Long userId) {
        Long operatorUserId = CurrentUser.requireUserId();
        adminAccountApplicationService.disableAdminAccount(userId, operatorUserId);
        return ApiResponse.ok();
    }

    @PostMapping("/{userId}/password")
    public ApiResponse<Void> changePassword(
        @PathVariable("userId") Long userId,
        @Valid @RequestBody AdminChangePasswordRequest request) {
        adminAccountApplicationService.changeAdminPassword(new ChangeAdminPasswordCommand(userId, request.newPassword()));
        return ApiResponse.ok();
    }

    @PostMapping("/password/validate")
    public ApiResponse<Void> validatePasswordStrength(@Valid @RequestBody AdminPasswordStrengthRequest request) {
        adminAccountApplicationService.validatePasswordStrength(request.password(), request.username());
        return ApiResponse.ok();
    }
}
