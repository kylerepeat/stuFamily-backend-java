package com.stufamily.backend.shared.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void shouldBuildSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("ok");
        assertTrue(response.success());
        assertEquals("ok", response.data());
    }

    @Test
    void shouldBuildFailureResponse() {
        ApiResponse<Void> response = ApiResponse.failure("ERR", "bad");
        assertEquals("ERR", response.code());
        assertEquals("bad", response.message());
    }
}

