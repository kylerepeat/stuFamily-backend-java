package com.stufamily.backend.adminquery.application.dto;

public record AdminMonthlyAmountView(
    String month,
    Long amountCents
) {
}
