package com.stufamily.backend.identity.application.command;

public record ChangeAdminPasswordCommand(
    Long userId,
    String newPassword
) {
}
