package com.stufamily.backend.weixinapi.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateParentMessageRequest(
    @NotBlank @Size(max = 500) String content
) {
}

