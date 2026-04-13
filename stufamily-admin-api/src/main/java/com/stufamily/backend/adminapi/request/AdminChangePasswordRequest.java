package com.stufamily.backend.adminapi.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminChangePasswordRequest(
    @NotBlank @Size(min = 8, max = 32) String newPassword
) {
}
