package com.stufamily.backend.wechat.gateway.dto;

public record WechatPayNotifyResult(
    String outTradeNo,
    String transactionId,
    long totalAmountCents
) {
}
