package com.stufamily.backend.shared.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.stufamily.backend.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.DataBinder;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleBusinessException() {
        var response = handler.handleBusiness(new BusinessException(ErrorCode.LOGIN_FAILED, "bad"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertEquals("LOGIN_FAILED", body.code());
    }

    @Test
    void shouldHandleUnknownException() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("/test");
        var response = handler.handleOthers(new RuntimeException("x"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void shouldHandleValidationException() {
        BindException bindException = new BindException(new DataBinder(new Object()).getBindingResult());
        var response = handler.handleBadRequest(bindException);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldHandleAuthenticationException() {
        var response = handler.handleAuth(new BadCredentialsException("bad"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldHandleAccessDeniedException() {
        var response = handler.handleDenied(new AccessDeniedException("no"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
