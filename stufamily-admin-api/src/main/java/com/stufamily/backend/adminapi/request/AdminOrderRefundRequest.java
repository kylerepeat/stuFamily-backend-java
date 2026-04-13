package com.stufamily.backend.adminapi.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminOrderRefundRequest(
    @NotNull @Min(1) Long refundAmountCents,
    @Size(max = 255) String reason
) {
}
