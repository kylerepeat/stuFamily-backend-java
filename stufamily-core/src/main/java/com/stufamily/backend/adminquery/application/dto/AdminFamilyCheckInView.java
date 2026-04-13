package com.stufamily.backend.adminquery.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminFamilyCheckInView(
    Long id,
    String checkinNo,
    Long groupId,
    String groupNo,
    Long ownerUserId,
    String ownerOpenid,
    String ownerNickname,
    Long familyMemberId,
    String familyMemberNo,
    String familyMemberName,
    BigDecimal latitude,
    BigDecimal longitude,
    String addressText,
    OffsetDateTime checkedInAt,
    OffsetDateTime createdAt
) {
}
