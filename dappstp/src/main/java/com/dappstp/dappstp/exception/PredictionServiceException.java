package com.dappstp.dappstp.exception;

public class PredictionServiceException extends RuntimeException {

    public PredictionServiceException(String message) {
        super(message);
    }

    public PredictionServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
