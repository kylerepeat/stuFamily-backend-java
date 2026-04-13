package com.stufamily.backend.home.application.dto;

import java.time.OffsetDateTime;

public record AdminHomepageBannerView(
    Long id,
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
