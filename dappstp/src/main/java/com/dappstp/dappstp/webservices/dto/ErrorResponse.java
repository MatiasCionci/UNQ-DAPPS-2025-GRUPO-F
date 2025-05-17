package com.dappstp.dappstp.webservices.dto;

public class ErrorResponse {
    private final String error;
    private final long timestamp;

    public ErrorResponse(String error) {
        this.error = error;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getError() { return error; }
    public long getTimestamp() { return timestamp; }
}