package com.stufamily.backend.weixinapi.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AddFamilyMemberRequest(
    String groupNo,
    @NotBlank String memberName,
    @NotBlank String studentOrCardNo,
    @NotBlank String phone,
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime joinedAt
) {
}
