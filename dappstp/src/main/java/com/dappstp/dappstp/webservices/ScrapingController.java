package com.dappstp.dappstp.webservices; // Asegúrate que el paquete sea correcto para tu estructura
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dappstp.dappstp.model.Players;
import org.springframework.http.HttpStatus;
import com.dappstp.dappstp.service.scraping.clfinal.ScraperPlayersService;
import com.dappstp.dappstp.service.scraping.clfinal.SimpleScorePredictionScraperService;
import com.dappstp.dappstp.service.scraping.clfinal.TeamCharacteristicsScraperService;
import com.dappstp.dappstp.config.ApiPaths; // Asumiendo que tienes ApiPaths
import com.dappstp.dappstp.webservices.dto.ErrorResponse; // Para respuestas de error

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping(ApiPaths.API_BASE + "/scrape") // Usando ApiPaths y una ruta más específica
@Tag(name = "Scraping", description = "Endpoints para realizar scraping de diversas fuentes de datos deportivos.")
public class ScrapingController {

    private final ScraperPlayersService scraperService;
    private final SimpleScorePredictionScraperService scorePredictionScraperService;
    // completo de prediccion,dice los sucesos del partido
    private final TeamCharacteristicsScraperService teamCharacteristicsScraperService;
    


    private static final Logger logger = LoggerFactory.getLogger(ScrapingController.class);

    // Inyección de dependencias vía constructor para ambos servicios
   // @Autowired
    public ScrapingController(ScraperPlayersService scraperService,
                              SimpleScorePredictionScraperService scorePredictionScraperService,
                              TeamCharacteristicsScraperService teamCharacteristicsScraperService
                            ) {
     
        this.teamCharacteristicsScraperService = teamCharacteristicsScraperService;
        this.scorePredictionScraperService = scorePredictionScraperService;
 
        this.scraperService = scraperService;
    }

    @GetMapping("/status") // Cambiado de "/" a algo más descriptivo
    @Operation(summary = "Verificar estado de la API de Scraping",
               description = "Devuelve un mensaje indicando que los endpoints de scraping están disponibles.")
    public String index() {
        return "API DappSTP - Endpoints de Scraping disponibles.";
    }

    @GetMapping("/players")
    @Operation(summary = "Scrapear y guardar jugadores",
               description = "Realiza scraping de jugadores de una fuente predefinida, los guarda en la base de datos y devuelve un resumen.")
    @ApiResponse(responseCode = "200", description = "Jugadores scrapeados y procesados exitosamente.", content = @Content(mediaType = "text/plain"))
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
    @Operation(summary = "Scrapear predicción de resultado simple",
               description = "Realiza scraping de una predicción de resultado (marcador) desde una URL predefinida de WhoScored.")
    @ApiResponse(responseCode = "200", description = "Predicción de resultado obtenida exitosamente.", content = @Content(mediaType = "text/plain"))
    @ApiResponse(responseCode = "500", description = "Error interno al obtener la predicción.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
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
    //realmadrid getafe
     @GetMapping("/teamcharacteristics")
    @Operation(summary = "Scrapear características de equipos",
               description = "Realiza scraping de las características (fortalezas/debilidades) de los equipos desde una URL predefinida de WhoScored.")
    @ApiResponse(responseCode = "200", description = "Características de equipos obtenidas exitosamente.",
                 content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(type = "string"))))
    @ApiResponse(responseCode = "500", description = "Error interno al obtener las características.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<String>> getTeamCharacteristics() {
        try {
            List<String> characteristics = teamCharacteristicsScraperService.scrapeTeamCharacteristics("https://es.whoscored.com/matches/1821739/preview");
            return ResponseEntity.ok(characteristics);
        } catch (RuntimeException e) {
            logger.error("🚨 Error al ejecutar scrapeTeamCharacteristics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of("Error al obtener características: " + e.getMessage()));
        }
    }
   
    @GetMapping("/hello")
    @Operation(summary = "Endpoint de prueba 'Hola'", description = "Un simple endpoint para verificar que el controlador está funcionando.")
     public String hello() {
       return "Hola desde el backend!";
    }
}
