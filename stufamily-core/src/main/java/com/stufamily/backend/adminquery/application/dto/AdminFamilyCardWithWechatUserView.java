package com.stufamily.backend.adminquery.application.dto;

import java.time.OffsetDateTime;

public record AdminFamilyCardWithWechatUserView(
    Long familyGroupId,
    String groupNo,
    Long sourceOrderId,
    Long ownerUserId,
    Integer maxMembers,
    Integer currentMembers,
    String status,
    OffsetDateTime activatedAt,
    OffsetDateTime expireAt,
    OffsetDateTime createdAt,
    String ownerOpenid,
    String ownerNickname,
    String ownerAvatarUrl
) {
}
