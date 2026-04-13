package com.stufamily.backend.family.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FamilyCheckInView(
    String checkinNo,
    String groupNo,
    Long checkinUserId,
    Long familyMemberId,
    BigDecimal latitude,
    BigDecimal longitude,
    String addressText,
    OffsetDateTime checkedInAt
) {
}
