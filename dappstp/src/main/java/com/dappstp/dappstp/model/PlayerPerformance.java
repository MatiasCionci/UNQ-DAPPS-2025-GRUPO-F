package com.dappstp.dappstp.model;

import java.util.Map;

public class PlayerPerformance {
    private String playerId;
    private Map<String, Double> metrics;

    public PlayerPerformance() {}

    public PlayerPerformance(String playerId, Map<String, Double> metrics) {
        this.playerId = playerId;
        this.metrics = metrics;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public Map<String, Double> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Double> metrics) {
        this.metrics = metrics;
    }
}
