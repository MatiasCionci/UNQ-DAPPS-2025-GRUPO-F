package com.dappstp.dappstp.webservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dappstp.dappstp.service.scraping.clfinal.CLFinalTeamStatsSummaryScraperService;
import com.dappstp.dappstp.service.scraping.clfinal.dto.TeamStatsSummaryDto;

@RestController
@RequestMapping("/api/stats/cl-final")
@Slf4j
public class CLFinalStatsController {

    private final CLFinalTeamStatsSummaryScraperService clFinalScraperService;

    public CLFinalStatsController(CLFinalTeamStatsSummaryScraperService clFinalScraperService) {
        this.clFinalScraperService = clFinalScraperService;
    }

    @GetMapping("/scrape")
    public ResponseEntity<?> scrapeCLFinalStats() {
        String matchUrl = "https://es.whoscored.com/matches/1899310/teamstatistics/europa-champions-league-2024-2025-paris-saint-germain-inter";
        log.info("Solicitud recibida para scrapear estadísticas de la final de CL desde: {}", matchUrl);
        try {
            TeamStatsSummaryDto statsSummary = clFinalScraperService.scrapeTeamStatsSummary(matchUrl);
            log.info("Scraping completado exitosamente para la final de CL.");
            return ResponseEntity.ok(statsSummary);
        } catch (RuntimeException e) {
            log.error("Error al scrapear las estadísticas de la final de CL: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar la solicitud de scraping: " + e.getMessage());
        }
    }
}