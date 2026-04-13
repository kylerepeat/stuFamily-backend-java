package com.stufamily.backend.weixinapi.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateOrderRequest(
    @NotBlank String productType,
    @NotNull Long productId,
    Long skuId,
    String durationType,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate cardApplyDate,
    @Size(max = 64) String applicantName,
    @Size(max = 64) String applicantStudentOrCardNo,
    @Pattern(regexp = "^1\\d{10}$") String applicantPhone,
    @NotNull @Min(1) Long amountCents
) {
}
