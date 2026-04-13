package com.stufamily.backend.home.application.command;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminProductUpdateCommand(
    String productType,
    String title,
    String subtitle,
    String detailContent,
    List<String> imageUrls,
    String contactName,
    String contactPhone,
    OffsetDateTime serviceStartAt,
    OffsetDateTime serviceEndAt,
    OffsetDateTime saleStartAt,
    OffsetDateTime saleEndAt,
    String publishStatus,
    Boolean top,
    Integer displayPriority,
    List<FamilyCardPlanCommand> familyCardPlans,
    List<ValueAddedSkuCommand> valueAddedSkus
) {
    public record FamilyCardPlanCommand(
        Long id,
        String durationType,
        Integer durationMonths,
        Long priceCents,
        Integer maxFamilyMembers,
        Boolean enabled
    ) {
    }

    public record ValueAddedSkuCommand(
        Long id,
        String title,
        Long priceCents,
        Boolean enabled
    ) {
    }
}
