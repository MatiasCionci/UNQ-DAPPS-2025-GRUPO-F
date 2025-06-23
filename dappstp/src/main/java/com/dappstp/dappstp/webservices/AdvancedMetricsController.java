package com.dappstp.dappstp.webservices;

import com.dappstp.dappstp.dto.metricasAvanzadas.AdvancedTeamStatsDto;
import com.dappstp.dappstp.dto.metricasAvanzadas.TeamDataDto;
import com.dappstp.dappstp.service.advancedmetrics.AdvancedMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // Solución 1: Añadir la anotación @RestController
@RequestMapping("/api/teams/stats")
@RequiredArgsConstructor
@Slf4j // Añadir para poder usar 'log'
public class AdvancedMetricsController {

    private final AdvancedMetricsService advancedMetricsService;

    /**
     * Endpoint para obtener las estadísticas de resumen (datos brutos) de un equipo desde una URL de WhoScored.
     * @param url La URL completa de la página del equipo.
     * @return Un DTO con las estadísticas del equipo.
     */
    @GetMapping("/summary")
    public ResponseEntity<TeamDataDto> getTeamDataSummary(@RequestParam String url) {
        log.info("Request to scrape team stats summary from URL: {}", url); // Solución 2: Usar logger en vez de System.out
        TeamDataDto stats = advancedMetricsService.scrapeTeamData(url);
        return ResponseEntity.ok(stats);
    }

    /**
     * Endpoint para obtener las métricas avanzadas de un equipo desde una URL de WhoScored.
     * @param url La URL completa de la página del equipo.
     * @return Un DTO con las métricas avanzadas del equipo.
     */
    @GetMapping("/advanced-metrics")
    public ResponseEntity<AdvancedTeamStatsDto> getTeamAdvancedMetrics(@RequestParam String url) {
        log.info("Request to generate advanced team metrics from URL: {}", url);
        AdvancedTeamStatsDto advancedStats = advancedMetricsService.generateAdvancedTeamMetrics(url);
        return ResponseEntity.ok(advancedStats);
    }
}