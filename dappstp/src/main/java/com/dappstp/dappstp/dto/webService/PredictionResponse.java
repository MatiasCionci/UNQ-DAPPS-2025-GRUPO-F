package com.dappstp.dappstp.dto.webService;

import com.dappstp.dappstp.model.Prediction;

public class PredictionResponse {
    private final Prediction prediction;
    private final String status;
    private final long timestamp;

    public PredictionResponse(Prediction prediction, String status, long timestamp) {
        this.prediction = prediction;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters
    public Prediction getPrediction() { return prediction; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
}