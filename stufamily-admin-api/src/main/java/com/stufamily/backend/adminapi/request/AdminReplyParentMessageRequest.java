package com.stufamily.backend.adminapi.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReplyParentMessageRequest(
    @NotBlank @Size(max = 500) String content
) {
}

