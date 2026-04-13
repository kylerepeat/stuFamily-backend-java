package com.stufamily.backend.family.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record AddFamilyMemberCommand(
    @NotNull Long ownerUserId,
    String groupNo,
    @NotBlank String memberName,
    @NotBlank String studentOrCardNo,
    @NotBlank String phone,
    @NotNull OffsetDateTime joinedAt
) {
}
