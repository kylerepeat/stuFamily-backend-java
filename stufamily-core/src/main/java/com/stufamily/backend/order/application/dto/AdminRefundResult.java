package com.stufamily.backend.order.application.dto;

import java.time.OffsetDateTime;

public record AdminRefundResult(
    String orderNo,
    String refundNo,
    String refundStatus,
    String wechatRefundId,
    Long refundAmountCents,
    Long refundedAmountCents,
    Long remainRefundableAmountCents,
    String orderStatus,
    String paymentStatus,
    OffsetDateTime refundAt
) {
}
