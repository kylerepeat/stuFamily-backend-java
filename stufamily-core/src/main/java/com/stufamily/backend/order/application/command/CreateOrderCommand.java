package com.stufamily.backend.order.application.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateOrderCommand(
    @NotNull Long userId,
    @NotBlank String productType,
    @NotNull Long productId,
    Long skuId,
    String durationType,
    LocalDate cardApplyDate,
    String applicantName,
    String applicantStudentOrCardNo,
    String applicantPhone,
    @NotNull @Min(1) Long amountCents,
    String clientIp
) {
}
