package com.stufamily.backend.home.application.command;

import java.time.OffsetDateTime;

public record AdminNoticeCreateCommand(
    String title,
    String content,
    Boolean enabled,
    Integer sortOrder,
    OffsetDateTime startAt,
    OffsetDateTime endAt
) {
}
