package com.neueda.leap.exception;

import com.neueda.leap.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @SuppressWarnings("java:S2259")
    void handleInvalidTradeReturnsBadRequest() {
        HttpServletRequest request = new MockHttpServletRequest("POST", "/api/trades/record");

        var response = globalExceptionHandler.handleInvalidTrade(
                new InvalidTradeException("Failed to record trade"),
                request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
