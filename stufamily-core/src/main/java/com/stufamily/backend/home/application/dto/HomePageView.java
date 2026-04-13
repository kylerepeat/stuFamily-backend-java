package com.stufamily.backend.home.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record HomePageView(
    List<BannerView> banners,
    String bannerSlogan,
    SiteProfileView siteProfile,
    List<NoticeView> notices
) {
    public record BannerView(Long id, String title, String imageUrl) {
    }

    public record SiteProfileView(
        String communityName,
        String introText,
        String contactPerson,
        String contactPhone,
        String contactWechat,
        String contactWechatQrUrl,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
    ) {
    }

    public record NoticeView(
        String title,
        String content
    ) {
    }

}
