package com.stufamily.backend.identity.application.command;

import jakarta.validation.constraints.NotBlank;

public record WechatLoginCommand(
    @NotBlank String code,
    String nickname,
    String avatarUrl
) {
}

