package com.stufamily.backend.home.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record HomeProductDetailView(
    Long id,
    String productNo,
    String type,
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
    boolean deleted,
    boolean top,
    int displayPriority,
    Long listVisibilityRuleId,
    Long detailVisibilityRuleId,
    Long categoryId,
    List<FamilyCardPlanView> familyCardPlans,
    List<ValueAddedSkuView> valueAddedSkus
) {
    public record FamilyCardPlanView(
        Long id,
        String durationType,
        int durationMonths,
        long priceCents,
        int maxFamilyMembers,
        boolean enabled
    ) {
    }

    public record ValueAddedSkuView(
        Long id,
        String title,
        long priceCents,
        boolean enabled
    ) {
    }
}
