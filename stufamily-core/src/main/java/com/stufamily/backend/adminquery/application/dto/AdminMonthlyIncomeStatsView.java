package com.stufamily.backend.adminquery.application.dto;

import java.util.List;

public record AdminMonthlyIncomeStatsView(
    List<AdminMonthlyAmountView> monthlyTotalIncome,
    List<AdminMonthlyAmountView> monthlyRefundIncome,
    Long totalIncomeCents,
    Long totalRefundCents,
    Long netIncomeCents
) {
}
