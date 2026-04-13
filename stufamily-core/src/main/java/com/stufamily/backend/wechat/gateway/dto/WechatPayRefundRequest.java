package com.stufamily.backend.wechat.gateway.dto;

public record WechatPayRefundRequest(
    String outTradeNo,
    String transactionId,
    String outRefundNo,
    Long totalAmountCents,
    Long refundAmountCents,
    String reason
) {
}
