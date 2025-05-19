package com.dappstp.dappstp.service;

import com.dappstp.dappstp.model.PlayerPerformance;
import java.util.List;

public interface WhoScoredScraper {
  PlayerPerformance scrapeRecentStats(String playerId);
  List<PlayerPerformance> scrapeHistoricalStats(String playerId);
}
