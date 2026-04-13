package com.stufamily.backend.home.application.dto;

import java.time.OffsetDateTime;

public record AdminParentMessageView(
    Long id,
    Long userId,
    String nickname,
    String avatarUrl,
    String content,
    Boolean viewed,
    Boolean replied,
    Boolean closed,
    OffsetDateTime createdAt,
    OffsetDateTime viewedAt,
    OffsetDateTime repliedAt
) {
}

