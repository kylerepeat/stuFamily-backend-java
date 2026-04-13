package com.stufamily.backend.order.application.command;

import jakarta.validation.constraints.NotBlank;

public record PayNotifyCommand(
    @NotBlank String outTradeNo,
    @NotBlank String transactionId,
    long totalAmountCents
) {
}

