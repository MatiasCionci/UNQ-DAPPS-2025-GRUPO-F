package com.dappstp.dappstp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.dappstp.dappstp.factory.PlayerPerformanceFactory;
import com.dappstp.dappstp.model.PerformanceAnalysis;
import com.dappstp.dappstp.service.impl.PlayerPerformanceServiceImpl;
import com.dappstp.dappstp.service.scraping.ScrapperPlayerPerformace;


class PlayerPerformanceServiceTest {
    @Mock
    private ScrapperPlayerPerformace scraper;

    @InjectMocks
    private PlayerPerformanceServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void analyzePlayerPerformance_returnsCorrectPerformanceIndex() {
        when(scraper.scrapeRecentStats("123")).thenReturn(PlayerPerformanceFactory.createRecentSample());
        when(scraper.scrapeHistoricalStats("123")).thenReturn(PlayerPerformanceFactory.createHistoricalSample());

        PerformanceAnalysis result = service.analyzePlayerPerformance("123");
        assertEquals(4.0, result.getPerformanceIndex());
    }

    @Test
    void analyzePlayerPerformance_withNoHistorical_trendEqualsRecentValue() {
        when(scraper.scrapeRecentStats("123")).thenReturn(PlayerPerformanceFactory.createRecentSample());
        when(scraper.scrapeHistoricalStats("123")).thenReturn(List.of());

        PerformanceAnalysis result = service.analyzePlayerPerformance("123");
        assertEquals(5.0, result.getTrend().get("goals"));
    }
}
