package com.stufamily.backend.weixinapi.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PayNotifyRequest(
    @NotBlank String outTradeNo,
    @NotBlank String transactionId,
    @Min(1) long totalAmountCents
) {
}

