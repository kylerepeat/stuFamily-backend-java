package com.stufamily.backend.weixinapi.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AddFamilyCheckInRequest(
    String groupNo,
    Long familyMemberId,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
    @NotBlank @Size(max = 255) String addressText,
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime checkedInAt
) {
}
