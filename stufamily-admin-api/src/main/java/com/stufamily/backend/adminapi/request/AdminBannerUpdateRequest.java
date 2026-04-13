package com.stufamily.backend.adminapi.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record AdminBannerUpdateRequest(
    @NotBlank String title,
    @NotBlank String imageUrl,
    String linkType,
    String linkTarget,
    @NotNull Integer sortOrder,
    @NotNull Boolean enabled,
    OffsetDateTime startAt,
    OffsetDateTime endAt
) {
}
