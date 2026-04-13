package com.stufamily.backend.home.application.command;

import java.math.BigDecimal;

public record AdminSiteProfileUpdateCommand(
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
