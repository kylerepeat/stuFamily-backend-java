package com.stufamily.backend.wechat.gateway.dto;

public record WechatPayCreateResponse(
    String prepayId,
    String nonceStr,
    String paySign,
    String timeStamp
) {
}

