package com.stufamily.backend.home.application.command;

public record CreateParentMessageCommand(
    Long userId,
    String content
) {
}

