package com.stufamily.backend.adminapi.controller;

import com.stufamily.backend.adminapi.request.AdminLoginRequest;
import com.stufamily.backend.identity.application.command.AdminLoginCommand;
import com.stufamily.backend.identity.application.dto.LoginResult;
import com.stufamily.backend.identity.application.service.AuthApplicationService;
import com.stufamily.backend.shared.api.ApiResponse;
import com.stufamily.backend.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AuthApplicationService authApplicationService;

    public AdminAuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(authApplicationService.adminLogin(
            new AdminLoginCommand(request.username(), request.password()))
        );
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authApplicationService.adminLogout(CurrentUser.requireUserId());
        return ApiResponse.ok();
    }
}
