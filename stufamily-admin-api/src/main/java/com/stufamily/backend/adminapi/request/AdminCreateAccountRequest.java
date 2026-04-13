package com.stufamily.backend.adminapi.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreateAccountRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 8, max = 32) String password,
    String nickname,
    String phone,
    @Email String email
) {
}
