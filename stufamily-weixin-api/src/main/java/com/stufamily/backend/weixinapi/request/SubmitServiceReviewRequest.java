package com.stufamily.backend.weixinapi.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitServiceReviewRequest(
    @NotNull @Min(1) @Max(5) Integer stars,
    @Size(max = 500) String content
) {
}
