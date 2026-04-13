package com.stufamily.backend.order.application.dto;

import java.time.OffsetDateTime;

public record AdminDisableFamilyGroupResult(
    String orderNo,
    String groupNo,
    String groupStatus,
    int totalMemberCount,
    int disabledMemberCount,
    OffsetDateTime operatedAt
) {
}
