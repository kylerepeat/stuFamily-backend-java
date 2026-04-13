package com.stufamily.backend.shared.security;

import com.stufamily.backend.shared.exception.BusinessException;
import com.stufamily.backend.shared.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "user not authenticated");
        }
        return principal.getUserId();
    }
}

