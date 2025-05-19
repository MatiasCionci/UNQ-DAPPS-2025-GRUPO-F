package com.dappstp.dappstp.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.dappstp.dappstp.model.PerformanceAnalysis;
import com.dappstp.dappstp.model.PlayerPerformance;
import com.dappstp.dappstp.service.PlayerPerformanceService;
import com.dappstp.dappstp.service.WhoScoredScraper;

@Service
public class PlayerPerformanceServiceImpl implements PlayerPerformanceService {
    private final WhoScoredScraper scraper;

    public PlayerPerformanceServiceImpl(WhoScoredScraper scraper) {
        this.scraper = scraper;
    }

    @Override
    public PerformanceAnalysis analyzePlayerPerformance(String playerId) {
        PlayerPerformance recent = scraper.scrapeRecentStats(playerId);
        List<PlayerPerformance> hist   = scraper.scrapeHistoricalStats(playerId);

        double idx = recent.getMetrics().values().stream()
                          .mapToDouble(d -> d).average().orElse(0);

        Map<String, Double> histSum = new HashMap<>();
        for (PlayerPerformance ps : hist) {
            ps.getMetrics().forEach((k, v) ->
                histSum.merge(k, v, Double::sum)
            );
        }
        int count = hist.isEmpty() ? 1 : hist.size();
        Map<String, Double> avgHist = new HashMap<>();
        histSum.forEach((k,v) -> avgHist.put(k, v / count));

        Map<String, Double> trend = new HashMap<>();
        recent.getMetrics().forEach((k, v) ->
            trend.put(k, v - avgHist.getOrDefault(k, 0.0))
        );

        return new PerformanceAnalysis(playerId, idx, trend);
    }
}
