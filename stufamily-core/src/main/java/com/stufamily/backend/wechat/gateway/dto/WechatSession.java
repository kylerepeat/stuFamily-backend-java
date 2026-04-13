package com.stufamily.backend.wechat.gateway.dto;

public record WechatSession(
    String openid,
    String unionid,
    String sessionKey
) {
}

