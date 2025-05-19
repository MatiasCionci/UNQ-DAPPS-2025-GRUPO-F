package com.dappstp.dappstp.factory;

import java.util.List;
import java.util.Map;

import com.dappstp.dappstp.model.PlayerPerformance;

public class PlayerPerformanceFactory {
    public static PlayerPerformance createRecentSample() {
        return new PlayerPerformance("123", Map.of("goals", 5.0, "assists", 3.0));
    }

    public static List<PlayerPerformance> createHistoricalSample() {
        return List.of(new PlayerPerformance("123", Map.of("goals", 4.0, "assists", 2.0)));
    }
}
