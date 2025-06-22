package com.dappstp.dappstp.webservices;
import com.dappstp.dappstp.model.Prediction;
import com.dappstp.dappstp.model.queryhistory.PredictionLog;
import com.dappstp.dappstp.service.predictionia.PredictionService;
import com.dappstp.dappstp.service.scraping.championsLeagueFinal.ComprehensivePredictionInputService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat; // Para parsear fechas
import org.springframework.web.bind.annotation.*;
import com.dappstp.dappstp.config.ApiPaths; // Importar la clase ApiPaths
import com.dappstp.dappstp.dto.webService.ErrorResponse;
import com.dappstp.dappstp.dto.webService.PredictionRequest;
import com.dappstp.dappstp.dto.webService.PredictionResponse;

import java.time.LocalDate; // Para el nuevo endpoint
import java.time.LocalDateTime;
import java.util.List; // Para el nuevo endpoint
@RestController
@RequestMapping(ApiPaths.PREDICTIONS_PP) // Usar la constante
@Tag(name = "Predictions", description = "Endpoints para generar y consultar predicciones de partidos.")
public class PredictionController {

    private static final Logger logger = LoggerFactory.getLogger(PredictionController.class);
    private final PredictionService predictionService;

    private final ComprehensivePredictionInputService comprehensivePredictionInputService; // Nuevo servicio

    // Inyección por constructor (recomendado)
   
    public PredictionController(PredictionService predictionService,                              
                                ComprehensivePredictionInputService comprehensivePredictionInputService) { // Inyectar nuevo servicio
        this.predictionService = predictionService;
        this.comprehensivePredictionInputService = comprehensivePredictionInputService;
    }

    @PostMapping
    @Operation(summary = "Generar una predicción simple",
               description = "Recibe datos scrapeados en formato de texto y devuelve una predicción.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos de entrada para la predicción.", required = true,
            content = @Content(schema = @Schema(implementation = PredictionRequest.class)))
    @ApiResponse(responseCode = "200", description = "Predicción generada exitosamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PredictionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<?> generatePrediction(@RequestBody PredictionRequest request) {
        logger.info("Solicitud de predicción recibida para: {}", request.getMatchId());
        
        try {
            // Validación básica
            if (request.getScrapedData() == null || request.getScrapedData().isBlank()) {
                logger.warn("Datos de entrada inválidos");
                return ResponseEntity.badRequest().body(
                    new ErrorResponse("Datos de análisis no pueden estar vacíos")
                );
            }

            Prediction prediction = predictionService.analyzeMatch(request.getScrapedData());
            
            return ResponseEntity.ok().body(
                new PredictionResponse(
                    prediction,
                    "success",
                    System.currentTimeMillis()
                )
            );
            
        } catch (Exception e) {
            logger.error("Error procesando solicitud: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                new ErrorResponse("Error generando predicción: " + e.getMessage())
            );
        }
    }

    @GetMapping("/generate-comprehensive")
    @Operation(summary = "Generar una predicción integral",
               description = "Recopila datos de múltiples fuentes (partidos de equipos, jugadores, estadísticas de finales) y genera una predicción. Esta predicción también se guarda en el historial.")
    @ApiResponse(responseCode = "200", description = "Predicción integral generada exitosamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PredictionResponse.class)))
    public ResponseEntity<?> generateComprehensivePrediction() {
        logger.info("Solicitud de predicción integral recibida.");
        try {
            // Ahora el servicio ComprehensivePredictionInputService también genera y loguea
            Prediction prediction = comprehensivePredictionInputService.generateAndLogComprehensivePrediction();
            logger.debug("Predicción integral generada y logueada.");
            
            return ResponseEntity.ok().body(
                new PredictionResponse(
                    prediction,
                    "success",
                    System.currentTimeMillis()
                )
            );
        } catch (Exception e) {
            logger.error("Error procesando solicitud de predicción integral: {}", e.getMessage(), e); // Añadimos 'e' para el stack trace en logs
            return ResponseEntity.internalServerError().body(
                new ErrorResponse("Error generando predicción integral: " + e.getMessage())
            );
        }
    }

    @GetMapping("/history")
    @Operation(summary = "Consultar historial de predicciones",
               description = "Permite consultar las predicciones integrales realizadas en una fecha específica.")
    @ApiResponse(responseCode = "200", description = "Historial de predicciones obtenido exitosamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PredictionLog.class, type = "array")))
    public ResponseEntity<?> getPredictionHistory(
            @Parameter(description = "Fecha para la cual consultar el historial (formato YYYY-MM-DD).", required = true, example = "2023-10-27")
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        // Por simplicidad, tomamos una sola fecha y buscamos en todo ese día.
        // Podrías extenderlo para aceptar un rango (startDate, endDate).
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);

        logger.info("Solicitud de historial de predicciones recibida para la fecha: {}", date);
        try {
            List<PredictionLog> history = comprehensivePredictionInputService.getPredictionHistory(startOfDay, endOfDay);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error al obtener el historial de predicciones para la fecha {}: {}", date, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                new ErrorResponse("Error obteniendo historial: " + e.getMessage())
            );
        }
    }
}
