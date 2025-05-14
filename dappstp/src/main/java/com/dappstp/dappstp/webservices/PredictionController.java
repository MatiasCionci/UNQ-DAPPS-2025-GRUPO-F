
package com.dappstp.dappstp.webservices;


import com.dappstp.dappstp.model.Prediction;
import com.dappstp.dappstp.service.prediction.PredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predictionspp")
public class PredictionController {

    private static final Logger logger = LoggerFactory.getLogger(PredictionController.class);
    private final PredictionService predictionService;

    // Inyección por constructor (recomendado)
    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
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

    // Clases DTO para request/response
    static class PredictionRequest {
        private String matchId;
        private String scrapedData;

        // Getters y Setters
        public String getMatchId() { return matchId; }
        public void setMatchId(String matchId) { this.matchId = matchId; }
        public String getScrapedData() { return scrapedData; }
        public void setScrapedData(String scrapedData) { this.scrapedData = scrapedData; }
    }

    static class PredictionResponse {
        private final Prediction prediction;
        private final String status;
        private final long timestamp;

        public PredictionResponse(Prediction prediction, String status, long timestamp) {
            this.prediction = prediction;
            this.status = status;
            this.timestamp = timestamp;
        }

        // Getters
        public Prediction getPrediction() { return prediction; }
        public String getStatus() { return status; }
        public long getTimestamp() { return timestamp; }
    }

    static class ErrorResponse {
        private final String error;
        private final long timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getError() { return error; }
        public long getTimestamp() { return timestamp; }
    }
}
