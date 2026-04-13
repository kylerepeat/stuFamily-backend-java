package com.stufamily.backend.family.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AddFamilyCheckInCommand(
    @NotNull Long ownerUserId,
    String groupNo,
    Long familyMemberId,
    @NotNull BigDecimal latitude,
    @NotNull BigDecimal longitude,
    @NotBlank String addressText,
    @NotNull OffsetDateTime checkedInAt
) {
}
