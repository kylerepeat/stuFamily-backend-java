package com.stufamily.backend.family.application.dto;

import java.time.OffsetDateTime;

public record FamilyMemberView(
    String memberNo,
    String memberName,
    String studentOrCardNo,
    String phone,
    OffsetDateTime joinedAt,
    String status,
    OffsetDateTime familyGroupExpireAt,
    String wechatAvatarUrl
) {
}
