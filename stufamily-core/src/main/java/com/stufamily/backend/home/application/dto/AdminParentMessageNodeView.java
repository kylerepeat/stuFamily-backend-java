package com.stufamily.backend.home.application.dto;

import java.time.OffsetDateTime;

public record AdminParentMessageNodeView(
    Long id,
    Long parentId,
    Long rootId,
    Long userId,
    String senderType,
    String nickname,
    String avatarUrl,
    String content,
    OffsetDateTime createdAt
) {
}

