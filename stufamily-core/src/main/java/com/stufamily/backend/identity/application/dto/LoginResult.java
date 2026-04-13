package com.stufamily.backend.identity.application.dto;

import java.util.List;

public record LoginResult(
    Long userId,
    String accessToken,
    String tokenType,
    String username,
    List<String> roles
) {
}

