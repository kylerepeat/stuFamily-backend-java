package com.stufamily.backend.identity.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWechatProfileCommand(
    @NotNull Long userId,
    @NotBlank @Size(max = 64) String nickname,
    @NotBlank String phone
) {
}
