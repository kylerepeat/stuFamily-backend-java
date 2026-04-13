package com.stufamily.backend.weixinapi.request;

import jakarta.validation.constraints.NotBlank;

public record WechatLoginRequest(
    @NotBlank String code,
    String nickname,
    String avatarUrl
) {
}

