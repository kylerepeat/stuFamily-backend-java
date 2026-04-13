package com.stufamily.backend.order.application.dto;

import java.time.OffsetDateTime;

public record PurchasedProductView(
    String orderNo,
    String orderType,
    String orderStatus,
    OffsetDateTime paidAt,
    Long productId,
    String productType,
    String productTitle,
    String productBrief,
    String productImageUrls,
    String selectedDurationType,
    Integer selectedDurationMonths,
    OffsetDateTime serviceStartAt,
    OffsetDateTime serviceEndAt,
    Long unitPriceCents,
    Integer quantity,
    Long totalPriceCents,
    Integer reviewStars,
    String reviewContent,
    OffsetDateTime reviewedAt
) {
}
