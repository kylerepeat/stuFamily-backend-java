package com.stufamily.backend.home.application.dto;

import java.time.OffsetDateTime;

public record AdminHomeNoticeView(
    Long id,
    String title,
    String content,
    Boolean enabled,
    Integer sortOrder,
    OffsetDateTime startAt,
    OffsetDateTime endAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
