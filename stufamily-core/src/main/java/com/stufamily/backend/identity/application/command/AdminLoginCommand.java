package com.stufamily.backend.identity.application.command;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginCommand(
    @NotBlank String username,
    @NotBlank String password
) {
}

