package com.dappstp.dappstp.webservices;
import com.dappstp.dappstp.model.Prediction;
import com.dappstp.dappstp.service.PlayersService;
import com.dappstp.dappstp.service.getapifootball.FootballApiService;
import com.dappstp.dappstp.service.predictionia.PredictionService;
import com.dappstp.dappstp.service.scraping.clfinal.CLFinalTeamStatsSummaryScraperService;
import com.dappstp.dappstp.service.scraping.clfinal.ComprehensivePredictionInputService;
import com.dappstp.dappstp.webservices.dto.ErrorResponse;
import com.dappstp.dappstp.webservices.dto.PredictionRequest;
import com.dappstp.dappstp.webservices.dto.PredictionResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predictionspp")
public class PredictionController {

    private static final Logger logger = LoggerFactory.getLogger(PredictionController.class);
    private final PredictionService predictionService;
    private final FootballApiService footballApiService;
    private final PlayersService playersService;
    private final CLFinalTeamStatsSummaryScraperService clFinalScraperService;
    private final ComprehensivePredictionInputService comprehensivePredictionInputService; // Nuevo servicio

    // Inyección por constructor (recomendado)
    @Autowired
    public PredictionController(PredictionService predictionService,
                                FootballApiService footballApiService,
                                PlayersService playersService,
                                CLFinalTeamStatsSummaryScraperService clFinalScraperService,
                                ComprehensivePredictionInputService comprehensivePredictionInputService) { // Inyectar nuevo servicio
        this.predictionService = predictionService;
        this.footballApiService = footballApiService;
        this.playersService = playersService;
        this.clFinalScraperService = clFinalScraperService;
        this.comprehensivePredictionInputService = comprehensivePredictionInputService;
    }

    @PostMapping
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
    public ResponseEntity<?> generateComprehensivePrediction() {
        logger.info("Solicitud de predicción integral recibida.");
        try {
            // Delegar la recolección y formato de datos al nuevo servicio
            String finalDataString = comprehensivePredictionInputService.aggregateDataForPrediction();
            logger.debug("String de datos integral generado: {}", finalDataString);

            Prediction prediction = predictionService.analyzeMatch(finalDataString);
            
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
}
