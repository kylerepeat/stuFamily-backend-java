package com.stufamily.backend.shared.exception;

import com.stufamily.backend.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure(ex.getErrorCode().code(), ex.getMessage()));
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        ConstraintViolationException.class,
        BindException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure(ErrorCode.INVALID_PARAM.code(), "request validation failed"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.failure(ErrorCode.UNAUTHORIZED.code(), "unauthorized"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.failure(ErrorCode.FORBIDDEN.code(), "forbidden"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOthers(Exception ex, HttpServletRequest request) {
        log.error("Unexpected exception on path: {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.failure(ErrorCode.SERVER_ERROR.code(), "internal server error"));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Bad request on path: {}", request.getRequestURI());
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure(ErrorCode.INVALID_PARAM.code(), "bad request"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        log.debug("Resource not found on path: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.failure(ErrorCode.INVALID_PARAM.code(), "resource not found"));
    }

}
