package com.stufamily.backend.adminapi.request;

import jakarta.validation.constraints.NotBlank;

public record AdminPasswordStrengthRequest(
    @NotBlank String password,
    String username
) {
}
