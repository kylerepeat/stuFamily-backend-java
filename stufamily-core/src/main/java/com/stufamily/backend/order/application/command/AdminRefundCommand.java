package com.stufamily.backend.order.application.command;

public record AdminRefundCommand(
    String orderNo,
    Long refundAmountCents,
    String reason,
    Long operatorUserId
) {
}
