package com.dappstp.dappstp.WebService;

import java.util.List;
import java.util.Map; // Importar Map

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // Importar HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping; // Importar PostMapping (alternativa)
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Quita esta importación si no la usas directamente aquí
// import com.dappstp.dappstp.model.Player;
import com.dappstp.dappstp.model.PlayerBarcelona;
// Quita esta importación si no la usas directamente aquí
// import com.dappstp.dappstp.model.PlayerProfileScraping;
import com.dappstp.dappstp.service.Scraping.PlayerProfileScrapingService;
import com.dappstp.dappstp.service.Scraping.ScraperServicePlayers;

import org.springframework.web.bind.annotation.PathVariable; // Para leer el nombre del equipo de la URL

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter; // Para describir el parámetro en Swagger
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api") // Ruta base para todos los endpoints de esta API
public class ScrapingController {

    private final ScraperServicePlayers scraperServicePlayers;
    private final PlayerProfileScrapingService playerProfileService;

    // Inyección de dependencias vía constructor para ambos servicios
    @Autowired
    public ScrapingController(ScraperServicePlayers scraperServicePlayers,
                              PlayerProfileScrapingService playerProfileService) {
        this.scraperServicePlayers = scraperServicePlayers;
        this.playerProfileService = playerProfileService;
    }

    @GetMapping("/players")
    public ResponseEntity<List<PlayerBarcelona>> scrapePlayers() {
        List<PlayerBarcelona> players = scraperServicePlayers.scrapeAndSavePlayers();
        return ResponseEntity.ok(players);
    }
    @GetMapping("/")
    public String index() {
        return "API DappSTP - Endpoints de Scraping disponibles en /api/scrape/...";
    }

    /**
     * Endpoint para iniciar el scraping de la lista de jugadores del Barcelona
     * y guardarlos en la base de datos.
     * Devuelve la lista de jugadores scrapeados.
     *
     * Alternativa: Usar @PostMapping si se considera una acción que modifica el estado.
     */
    @PostMapping("/scrape/players") // Usa esta línea
    // @PostMapping("/scrape/players") // Alternativa semánticamente más correcta
    public ResponseEntity<?> scrapeBarcelonaPlayers() {
        System.out.println("➡️ Endpoint /api/scrape/players invocado");
        try {
            List<PlayerBarcelona> players = scraperServicePlayers.scrapeAndSavePlayers();
            System.out.println("✅ Endpoint /api/scrape/players completado. Jugadores procesados: " + players.size());

            // Opción 1: Devolver la lista de jugadores scrapeados
            return ResponseEntity.ok(players);

            // Opción 2: Devolver solo un mensaje de éxito
            // return ResponseEntity.ok(Map.of(
            //     "message", "Scraping completado.",
            //     "players_processed", players.size()
            // ));

        } catch (Exception e) {
            System.err.println("❌ Error en endpoint /api/scrape/players: " + e.getMessage());
            e.printStackTrace(); // Loguear el error completo en el servidor
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Ocurrió un error durante el scraping de jugadores.", "details", e.getMessage()));
        }
    }

    /**
     * Endpoint para iniciar el scraping del perfil específico (actualmente Lamine Yamal)
     * y guardarlo en la base de datos.
     * Devuelve un mensaje de confirmación.
     *
     * Alternativa: Usar @PostMapping.
     * Futuro: Podría aceptar un ID de jugador como parámetro: /scrape/profile/{playerId}
     */
    @GetMapping("/scrape/profile/yamal")
    // @PostMapping("/scrape/profile/yamal") // Alternativa
    public ResponseEntity<Map<String, String>> scrapeYamalProfile() {
        System.out.println("➡️ Endpoint /api/scrape/profile/yamal invocado");
        try {
            // Llama al servicio que está hardcodeado para Yamal
            playerProfileService.scrapeAndSavePlayer();

            System.out.println("✅ Endpoint /api/scrape/profile/yamal completado.");
            return ResponseEntity.ok(Map.of("message", "Scraping del perfil de Lamine Yamal completado y guardado."));

        } catch (Exception e) {
            System.err.println("❌ Error en endpoint /api/scrape/profile/yamal: " + e.getMessage());
            e.printStackTrace(); // Loguear el error completo en el servidor
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Ocurrió un error durante el scraping del perfil.", "details", e.getMessage()));
        }
    }

   
    @GetMapping("/hello")
     public String hello() {
       return "Hola desde el backend!";
    }
}


