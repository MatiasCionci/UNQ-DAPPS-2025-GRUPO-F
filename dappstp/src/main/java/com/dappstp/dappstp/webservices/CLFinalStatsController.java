package com.dappstp.dappstp.webservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dappstp.dappstp.service.scraping.clfinal.CLFinalTeamStatsSummaryScraperService;
import com.dappstp.dappstp.service.scraping.clfinal.dto.TeamStatsSummaryDto;
import com.dappstp.dappstp.config.ApiPaths; // Asumiendo que tienes ApiPaths
import com.dappstp.dappstp.webservices.dto.ErrorResponse; // Para respuestas de error

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(ApiPaths.API_BASE + "/stats/cl-final") // Usando ApiPaths
@Tag(name = "Champions League Final Stats", description = "Endpoints para scrapear estadísticas de la final de la Champions League.")
@Slf4j
public class CLFinalStatsController {

    private final CLFinalTeamStatsSummaryScraperService clFinalScraperService;

    public CLFinalStatsController(CLFinalTeamStatsSummaryScraperService clFinalScraperService) {
        this.clFinalScraperService = clFinalScraperService;
    }

    @GetMapping("/scrape")
    @Operation(summary = "Scrapear estadísticas de la final de la CL",
               description = "Realiza scraping de las estadísticas de equipo para un partido predefinido de la final de la Champions League y las guarda en la base de datos.")
    @ApiResponse(responseCode = "200", description = "Estadísticas scrapeadas y guardadas exitosamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamStatsSummaryDto.class)))
    @ApiResponse(responseCode = "500", description = "Error interno durante el scraping.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<?> scrapeCLFinalStats() {
        String matchUrl = "https://es.whoscored.com/matches/1899310/teamstatistics/europa-champions-league-2024-2025-paris-saint-germain-inter";
        log.info("Solicitud recibida para scrapear estadísticas de la final de CL desde: {}", matchUrl);
        try {
            TeamStatsSummaryDto statsSummary = clFinalScraperService.scrapeTeamStatsSummary(matchUrl);
            log.info("Scraping completado exitosamente para la final de CL.");
            return ResponseEntity.ok(statsSummary);
        } catch (RuntimeException e) {
            log.error("Error al scrapear las estadísticas de la final de CL: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Error al procesar la solicitud de scraping: " + e.getMessage()));
        }
    }
}