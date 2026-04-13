package com.stufamily.backend.weixinapi.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWechatProfileRequest(
    @NotBlank @Size(max = 64) String nickname,
    @NotBlank @Pattern(regexp = "^1\\d{10}$") String phone
) {
}
