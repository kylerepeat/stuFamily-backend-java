package com.stufamily.backend.weixinapi.controller;

import com.stufamily.backend.identity.application.command.WechatLoginCommand;
import com.stufamily.backend.identity.application.command.UpdateWechatProfileCommand;
import com.stufamily.backend.identity.application.dto.LoginResult;
import com.stufamily.backend.identity.application.dto.WechatUserProfileView;
import com.stufamily.backend.identity.application.service.AuthApplicationService;
import com.stufamily.backend.shared.api.ApiResponse;
import com.stufamily.backend.shared.security.CurrentUser;
import com.stufamily.backend.weixinapi.request.UpdateWechatProfileRequest;
import com.stufamily.backend.weixinapi.request.WechatLoginRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weixin/auth")
public class WeixinAuthController {

    private final AuthApplicationService authApplicationService;

    public WeixinAuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody WechatLoginRequest request) {
        return ApiResponse.success(authApplicationService.wechatLogin(
            new WechatLoginCommand(request.code(), request.nickname(), request.avatarUrl()))
        );
    }

    @PutMapping("/profile")
    public ApiResponse<WechatUserProfileView> updateProfile(@Valid @RequestBody UpdateWechatProfileRequest request) {
        Long userId = CurrentUser.requireUserId();
        return ApiResponse.success(authApplicationService.updateWechatProfile(
            new UpdateWechatProfileCommand(userId, request.nickname(), request.phone())));
    }
}
