package com.stufamily.backend.adminapi.request;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {
}

