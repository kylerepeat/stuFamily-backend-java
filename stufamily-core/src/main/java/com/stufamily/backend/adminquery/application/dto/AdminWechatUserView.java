package com.stufamily.backend.adminquery.application.dto;

import java.time.OffsetDateTime;

public record AdminWechatUserView(
    Long userId,
    String userNo,
    String userType,
    String status,
    String openid,
    String nickname,
    String avatarUrl,
    String phone,
    OffsetDateTime lastLoginAt,
    OffsetDateTime createdAt
) {
}
