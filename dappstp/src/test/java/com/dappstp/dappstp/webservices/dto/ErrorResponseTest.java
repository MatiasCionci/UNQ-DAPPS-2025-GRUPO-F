package com.dappstp.dappstp.webservices.dto;

import org.junit.jupiter.api.Test;

import com.dappstp.dappstp.dto.webService.ErrorResponse;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void constructorAndGetters_shouldWorkCorrectly() {
        String errorMessage = "Test error message";
        long beforeTimestamp = System.currentTimeMillis();

        ErrorResponse errorResponse = new ErrorResponse(errorMessage);

        long afterTimestamp = System.currentTimeMillis();

        assertEquals(errorMessage, errorResponse.getError(), "Error message should match constructor argument");
        assertTrue(errorResponse.getTimestamp() >= beforeTimestamp, "Timestamp should be greater than or equal to beforeTimestamp");
        assertTrue(errorResponse.getTimestamp() <= afterTimestamp, "Timestamp should be less than or equal to afterTimestamp");
    }

    @Test
    void constructor_shouldSetTimestamp() {
        ErrorResponse errorResponse1 = new ErrorResponse("Error 1");
        // Introduce a small delay to ensure timestamps are different if System.currentTimeMillis() is called again
        try { Thread.sleep(10); } catch (InterruptedException e) { /* ignore */ }
        ErrorResponse errorResponse2 = new ErrorResponse("Error 2");

        assertTrue(errorResponse2.getTimestamp() > errorResponse1.getTimestamp(), "Timestamp should be set at the time of creation");
    }
}