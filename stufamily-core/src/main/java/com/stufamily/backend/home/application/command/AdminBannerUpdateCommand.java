package com.stufamily.backend.home.application.command;

import java.time.OffsetDateTime;

public record AdminBannerUpdateCommand(
    String title,
    String imageUrl,
    String linkType,
    String linkTarget,
    Integer sortOrder,
    Boolean enabled,
    OffsetDateTime startAt,
    OffsetDateTime endAt
) {
}
