package com.dappstp.dappstp.model;

import java.util.Map;

public class PerformanceAnalysis {
    private String playerId;
    private double performanceIndex;
    private Map<String, Double> trend;

    public PerformanceAnalysis() {}

    public PerformanceAnalysis(String playerId, double performanceIndex, Map<String, Double> trend) {
        this.playerId = playerId;
        this.performanceIndex = performanceIndex;
        this.trend = trend;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public double getPerformanceIndex() {
        return performanceIndex;
    }

    public void setPerformanceIndex(double performanceIndex) {
        this.performanceIndex = performanceIndex;
    }

    public Map<String, Double> getTrend() {
        return trend;
    }

    public void setTrend(Map<String, Double> trend) {
        this.trend = trend;
    }
}
