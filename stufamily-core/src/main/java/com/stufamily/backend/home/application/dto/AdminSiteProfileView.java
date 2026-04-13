package com.stufamily.backend.home.application.dto;

import java.math.BigDecimal;

public record AdminSiteProfileView(
    Long id,
    String communityName,
    String bannerSlogan,
    String introText,
    String contactPerson,
    String contactPhone,
    String contactWechat,
    String contactWechatQrUrl,
    String addressText,
    BigDecimal latitude,
    BigDecimal longitude,
    Boolean active
) {
}
