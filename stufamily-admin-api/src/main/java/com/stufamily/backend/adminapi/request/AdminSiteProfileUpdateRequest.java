package com.stufamily.backend.adminapi.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdminSiteProfileUpdateRequest(
    @NotBlank String communityName,
    String bannerSlogan,
    String introText,
    String contactPerson,
    String contactPhone,
    String contactWechat,
    String contactWechatQrUrl,
    String addressText,
    BigDecimal latitude,
    BigDecimal longitude,
    @NotNull Boolean active
) {
}
