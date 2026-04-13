package com.stufamily.backend.identity.application.dto;

public record WechatUserProfileView(
    Long userId,
    String nickname,
    String phone,
    String avatarUrl
) {
}
