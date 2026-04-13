package com.stufamily.backend.wechat.gateway.dto;

public record WechatPayCreateRequest(
    String outTradeNo,
    String openid,
    String body,
    long totalAmountCents,
    String notifyUrl,
    String clientIp
) {
}

