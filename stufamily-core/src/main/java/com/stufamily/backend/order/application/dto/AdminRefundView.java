package com.stufamily.backend.order.application.dto;

import java.time.OffsetDateTime;

public record AdminRefundView(
    String refundNo,
    String refundStatus,
    String wechatRefundId,
    Long refundAmountCents,
    String reason,
    OffsetDateTime successTime,
    OffsetDateTime createdAt
) {
}
