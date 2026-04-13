package com.stufamily.backend.adminquery.application.dto;

import java.time.OffsetDateTime;

public record AdminOrderWithWechatUserView(
    Long orderId,
    String orderNo,
    Long buyerUserId,
    String orderType,
    String orderStatus,
    Long payableAmountCents,
    String currency,
    OffsetDateTime createdAt,
    OffsetDateTime paidAt,
    String buyerOpenid,
    String buyerNickname,
    String buyerAvatarUrl
) {
}
