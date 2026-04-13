package com.stufamily.backend.adminapi.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminProductUpdateRequest(
    @NotBlank(message = "product_type is required")
    String productType,
    @NotBlank(message = "title is required")
    String title,
    String subtitle,
    @NotBlank(message = "detail_content is required")
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
    List<@Valid FamilyCardPlanRequest> familyCardPlans,
    List<@Valid ValueAddedSkuRequest> valueAddedSkus
) {
    public record FamilyCardPlanRequest(
        Long id,
        @NotBlank(message = "duration_type is required")
        String durationType,
        @NotNull(message = "duration_months is required")
        Integer durationMonths,
        @NotNull(message = "price_cents is required")
        Long priceCents,
        @NotNull(message = "max_family_members is required")
        Integer maxFamilyMembers,
        Boolean enabled
    ) {
    }

    public record ValueAddedSkuRequest(
        Long id,
        @NotBlank(message = "sku title is required")
        String title,
        @NotNull(message = "price_cents is required")
        Long priceCents,
        Boolean enabled
    ) {
    }
}
