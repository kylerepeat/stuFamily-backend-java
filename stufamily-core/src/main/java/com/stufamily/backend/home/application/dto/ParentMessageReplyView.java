package com.stufamily.backend.home.application.dto;

import java.time.OffsetDateTime;

public record ParentMessageReplyView(
    Long id,
    String senderType,
    String nickname,
    String avatarUrl,
    String content,
    OffsetDateTime createdAt
) {
}
