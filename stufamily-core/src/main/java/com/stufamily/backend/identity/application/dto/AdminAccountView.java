package com.stufamily.backend.identity.application.dto;

import java.time.OffsetDateTime;

public record AdminAccountView(
    Long id,
    String userNo,
    String username,
    String userType,
    String status,
    String nickname,
    String phone,
    String email,
    OffsetDateTime lastLoginAt,
    OffsetDateTime createdAt
) {
}
