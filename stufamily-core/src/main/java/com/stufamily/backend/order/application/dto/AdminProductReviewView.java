package com.stufamily.backend.order.application.dto;

import java.time.OffsetDateTime;

public record AdminProductReviewView(
    Long reviewId,
    Long orderId,
    String orderNo,
    Long buyerUserId,
    Long productId,
    String productType,
    Integer stars,
    String content,
    OffsetDateTime reviewedAt,
    OffsetDateTime createdAt
) {
}
