package com.dappstp.dappstp.service.advancedmetrics;
// En un nuevo archivo: AdvancedMetricsService.java
import org.springframework.stereotype.Service;
import com.dappstp.dappstp.dto.metricasAvanzadas.AdvancedPlayerStatsDto;
import com.dappstp.dappstp.dto.metricasAvanzadas.PlayerRawStatsDto;

@Service
public class AdvancedMetricsService {

    // Suponiendo que tienes un DTO con los datos brutos
    public AdvancedPlayerStatsDto calculatePlayerStats(PlayerRawStatsDto rawStats) {
        AdvancedPlayerStatsDto advancedStats = new AdvancedPlayerStatsDto();

        if (rawStats.getApps() > 0) {
            // Goles por partido
            double goalsPerGame = (double) rawStats.getGoals() / rawStats.getApps();
            advancedStats.setGoalsPerGame(goalsPerGame);

            // Tasa de conversión de tiros
            if (rawStats.getShotsPerGame() > 0) {
                double totalShots = rawStats.getShotsPerGame() * rawStats.getApps();
                if (totalShots > 0) {
                    double conversionRate = ((double) rawStats.getGoals() / totalShots) * 100;
                    advancedStats.setShotConversionRate(conversionRate);
                }
            }

            // Contribución de gol por partido (si tienes asistencias)
            int totalContributions = rawStats.getGoals() + rawStats.getAssists();
            double contributionsPerGame = (double) totalContributions / rawStats.getApps();
            advancedStats.setGoalContributionsPerGame(contributionsPerGame);
        }

        return advancedStats;
    }
}

// Nota: PlayerRawStatsDto sería un DTO que representa los datos que scrapeaste.

