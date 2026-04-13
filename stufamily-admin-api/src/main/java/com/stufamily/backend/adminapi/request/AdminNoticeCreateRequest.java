package com.stufamily.backend.adminapi.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record AdminNoticeCreateRequest(
    @NotBlank @Size(max = 50) String title,
    @Size(max = 2000) String content,
    Boolean enabled,
    Integer sortOrder,
    OffsetDateTime startAt,
    OffsetDateTime endAt
) {
}
