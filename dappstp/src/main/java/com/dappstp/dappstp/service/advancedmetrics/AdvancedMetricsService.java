package com.dappstp.dappstp.service.advancedmetrics;

import com.dappstp.dappstp.aspect.scraping.annotation.EnableScrapingSession;
import com.dappstp.dappstp.dto.metricasAvanzadas.AdvancedTeamStatsDto;
import com.dappstp.dappstp.dto.metricasAvanzadas.TeamDataDto;
import com.dappstp.dappstp.exception.ScrapingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdvancedMetricsService {

    private final TeamDataScraperService teamDataScraperService;

    /**
     * Scrapea los datos brutos de un equipo desde una URL y los devuelve.
     * Este método está anotado con @EnableScrapingSession para gestionar el WebDriver.
     *
     * @param teamUrl La URL de la página del equipo en WhoScored.
     * @return Un DTO con los datos brutos del equipo.
     * @throws ScrapingException Si ocurre un error durante el scraping.
     */
    @EnableScrapingSession
    public TeamDataDto scrapeTeamData(String teamUrl) {
        log.info("Iniciando scraping de datos de equipo para: {}", teamUrl);
        return teamDataScraperService.scrapeDataSummary(teamUrl);
    }

    /**
     * Orquesta el scraping de datos de un equipo y el cálculo de sus métricas avanzadas.
     *
     * @param teamUrl La URL de la página del equipo en WhoScored.
     * @return Un DTO con las métricas avanzadas del equipo.
     * @throws ScrapingException Si ocurre un error durante el scraping.
     */
    @EnableScrapingSession
    public AdvancedTeamStatsDto generateAdvancedTeamMetrics(String teamUrl) {
        log.info("Iniciando generación de métricas avanzadas para equipo desde: {}", teamUrl);
        // 1. Scrapear los datos brutos del equipo
        TeamDataDto rawTeamStats = teamDataScraperService.scrapeDataSummary(teamUrl);
        log.info("Datos brutos del equipo obtenidos: {}", rawTeamStats);

        // 2. Calcular las métricas avanzadas
        AdvancedTeamStatsDto advancedStats = calculateTeamStats(rawTeamStats);
        log.info("Métricas avanzadas del equipo calculadas: {}", advancedStats);

        return advancedStats;
    }

    /**
     * Calcula métricas avanzadas para un equipo a partir de sus estadísticas brutas.
     *
     * @param rawTeamStats DTO con las estadísticas brutas del equipo.
     * @return DTO con las métricas avanzadas calculadas.
     */
    public AdvancedTeamStatsDto calculateTeamStats(TeamDataDto rawTeamStats) {
        AdvancedTeamStatsDto advancedStats = new AdvancedTeamStatsDto();

        if (rawTeamStats.getApps() > 0) {
            // Goles por partido
            double goalsPerGame = (double) rawTeamStats.getGoals() / rawTeamStats.getApps();
            advancedStats.setGoalsPerGame(goalsPerGame);

            // Tasa de conversión de tiros
            if (rawTeamStats.getShotsPerGame() > 0) {
                double totalShots = rawTeamStats.getShotsPerGame() * rawTeamStats.getApps();
                if (totalShots > 0) {
                    double conversionRate = ((double) rawTeamStats.getGoals() / totalShots) * 100;
                    advancedStats.setShotConversionRate(conversionRate);
                }
            }

            // Tarjetas por partido
            int totalCards = rawTeamStats.getYellowCards() + rawTeamStats.getRedCards();
            double cardsPerGame = (double) totalCards / rawTeamStats.getApps();
            advancedStats.setCardsPerGame(cardsPerGame);
        }

        return advancedStats;
    }
}
