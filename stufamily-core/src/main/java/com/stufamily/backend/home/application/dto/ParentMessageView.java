package com.stufamily.backend.home.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ParentMessageView(
    Long id,
    String nickname,
    String avatarUrl,
    String content,
    OffsetDateTime createdAt,
    List<ParentMessageReplyView> replies
) {
}
