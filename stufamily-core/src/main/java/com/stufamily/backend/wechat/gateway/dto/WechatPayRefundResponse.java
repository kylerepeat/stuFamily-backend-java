package com.stufamily.backend.wechat.gateway.dto;

public record WechatPayRefundResponse(
    String wechatRefundId,
    String refundStatus,
    String returnCode,
    String resultCode,
    String errCode,
    String errCodeDesc
) {
}
