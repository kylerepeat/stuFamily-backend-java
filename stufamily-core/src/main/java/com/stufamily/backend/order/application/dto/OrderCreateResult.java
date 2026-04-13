package com.stufamily.backend.order.application.dto;

import com.stufamily.backend.wechat.gateway.dto.WechatPayCreateResponse;

public record OrderCreateResult(
    String orderNo,
    String status,
    long payableAmountCents,
    WechatPayCreateResponse payParams
) {
}

