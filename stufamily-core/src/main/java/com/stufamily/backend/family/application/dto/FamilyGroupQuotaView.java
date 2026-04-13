package com.stufamily.backend.family.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record FamilyGroupQuotaView(
    boolean hasActiveGroup,
    List<GroupQuotaView> groups
) {
    public static FamilyGroupQuotaView empty() {
        return new FamilyGroupQuotaView(false, List.of());
    }

    public record GroupQuotaView(
        String groupNo,
        String status,
        OffsetDateTime expireAt,
        int maxMembers,
        int currentMembers,
        int availableMembers,
        String productType,
        Long productId,
        String productTitle,
        Long planId,
        String durationType,
        Integer durationMonths
    ) {
    }
}
