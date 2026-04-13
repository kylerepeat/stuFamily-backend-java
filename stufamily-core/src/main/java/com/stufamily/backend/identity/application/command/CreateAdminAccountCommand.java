package com.stufamily.backend.identity.application.command;

public record CreateAdminAccountCommand(
    String username,
    String password,
    String nickname,
    String phone,
    String email
) {
}
