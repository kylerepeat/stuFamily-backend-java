package com.stufamily.backend.order.application.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitServiceReviewCommand(
    @NotNull Long userId,
    @jakarta.validation.constraints.NotBlank String orderNo,
    @NotNull @Min(1) @Max(5) Integer stars,
    @Size(max = 500) String content
) {
}
