package com.stufamily.backend.adminquery.application.dto;

import java.time.OffsetDateTime;

public record AdminFamilyMemberWithWechatUserView(
    Long memberId,
    String memberNo,
    String memberName,
    String studentOrCardNo,
    String phone,
    String memberStatus,
    OffsetDateTime joinedAt,
    Long familyGroupId,
    String groupNo,
    Long ownerUserId,
    String ownerNickname
) {
}

