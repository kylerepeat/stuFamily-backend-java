package com.stufamily.backend.shared.exception;

import com.stufamily.backend.shared.api.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${server.error.path:${error.path:/error}}")
public class ApiErrorController implements ErrorController {

    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> handleError(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ApiResponse.failure(resolveErrorCode(status).code(), resolveMessage(status)));
    }

    private HttpStatus resolveStatus(Object statusCodeAttribute) {
        if (statusCodeAttribute instanceof Integer statusCode) {
            HttpStatus httpStatus = HttpStatus.resolve(statusCode);
            if (httpStatus != null) {
                return httpStatus;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private ErrorCode resolveErrorCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ErrorCode.INVALID_PARAM;
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> ErrorCode.FORBIDDEN;
            default -> ErrorCode.SERVER_ERROR;
        };
    }

    private String resolveMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "request validation failed";
            case UNAUTHORIZED -> "unauthorized";
            case FORBIDDEN -> "forbidden";
            default -> "internal server error";
        };
    }
}
