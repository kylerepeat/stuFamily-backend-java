package com.stufamily.backend.shared.exception;

public enum ErrorCode {
    INVALID_PARAM("INVALID_PARAM"),
    UNAUTHORIZED("UNAUTHORIZED"),
    FORBIDDEN("FORBIDDEN"),
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION"),
    USER_NOT_FOUND("USER_NOT_FOUND"),
    LOGIN_FAILED("LOGIN_FAILED"),
    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS"),
    SERVER_ERROR("SERVER_ERROR");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
