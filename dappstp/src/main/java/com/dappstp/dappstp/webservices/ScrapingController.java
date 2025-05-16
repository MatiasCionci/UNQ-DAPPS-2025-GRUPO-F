package com.dappstp.dappstp.webservices; // Asegúrate que el paquete sea correcto para tu estructura
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.model.Prediction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.dappstp.dappstp.service.predictionia.PredictionService;
import com.dappstp.dappstp.service.scraping.clfinal.ScraperServicePlayers;
import com.dappstp.dappstp.service.scraping.clfinal.SimpleScorePredictionScraperService;
import com.dappstp.dappstp.service.scraping.clfinal.TeamCharacteristicsScraperService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api") // Ruta base para todos los endpoints de esta API
public class ScrapingController {

    private final ScraperServicePlayers scraperService;
    private final SimpleScorePredictionScraperService scorePredictionScraperService;
    // completo de prediccion,dice los sucesos del partido
    private final TeamCharacteristicsScraperService teamCharacteristicsScraperService;
    //prompt de dice la prediccion
    private final PredictionService predictionService;


    private static final Logger logger = LoggerFactory.getLogger(ScrapingController.class);

    // Inyección de dependencias vía constructor para ambos servicios
    @Autowired
    public ScrapingController(ScraperServicePlayers scraperService,
                              SimpleScorePredictionScraperService scorePredictionScraperService,
                              TeamCharacteristicsScraperService teamCharacteristicsScraperService,
                              PredictionService predictionService) {
        this.predictionService = predictionService;
        this.teamCharacteristicsScraperService = teamCharacteristicsScraperService;
        this.scorePredictionScraperService = scorePredictionScraperService;
 
        this.scraperService = scraperService;
    }

    @GetMapping("/")
    public String index() {
        return "API DappSTP - Endpoints de Scraping disponibles en /api/scrape/...";
    }

    @GetMapping("/players")
    public String getPlayers() {
        logger.info("➡️ Entró al endpoint /players");

        List<Players> players = scraperService.scrapeAndSavePlayers();
    
        logger.info("🧠 Scrap completado. Jugadores encontrados: {}", players.size());
    
        StringBuilder response = new StringBuilder("Jugadores:\n");
        players.forEach(p -> {
            response.append(p.getName())
                    .append(" - ")
                    .append(p.getMatches())
                    .append(" - ")
                    .append(p.getGoals())
                    .append(" - ")
                    .append(p.getAssists())
                    .append(" - ")
                    .append(p.getRating())
                    .append("\n");
        });
    
        return response.toString();
    }
    @GetMapping("/prediction")
    public ResponseEntity<String> prediction(){
               try {
            String resultado = scorePredictionScraperService.scrapeScorePrediction("https://es.whoscored.com/matches/1821739/preview/espa%C3%B1a-laliga-2024-2025-getafe-real-madrid");
            // Si el servicio devuelve una cadena indicando error internamente, aún así se devolverá como 200 OK.
            // Sería mejor que el servicio lance una excepción si algo va mal.
            return ResponseEntity.ok(resultado);
        } catch (Exception e) { // Idealmente, una excepción más específica de tu lógica de scraping
            logger.error("🚨 Error al ejecutar scrapeScorePrediction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener la predicción del resultado: " + e.getMessage());
        }
        
    }
     @GetMapping("/teamcharacteristics")
    public ResponseEntity<List<String>> getTeamCharacteristics() {
        try {
            List<String> characteristics = teamCharacteristicsScraperService.scrapeTeamCharacteristics("https://es.whoscored.com/matches/1821739/preview");
            return ResponseEntity.ok(characteristics);
        } catch (RuntimeException e) {
            // Loguear el error e.getMessage()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // O un mensaje de error más específico
        }
    }
   
    @GetMapping("/hello")
     public String hello() {
       return "Hola desde el backend!";
    }
}


