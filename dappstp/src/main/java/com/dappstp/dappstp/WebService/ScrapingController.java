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
    @Autowired
    private final ScraperServicePlayers scraperService;
    private final ScraperServicePlayers scraperServicePlayers;
    private final PlayerProfileScrapingService playerProfileService;

    // Inyección de dependencias vía constructor para ambos servicios
    @Autowired
    public ScrapingController(ScraperServicePlayers scraperServicePlayers,
                              PlayerProfileScrapingService playerProfileService,
                              ScraperServicePlayers scraperService) {
        this.scraperServicePlayers = scraperServicePlayers;
        this.playerProfileService = playerProfileService;
        this.scraperService = scraperService;
    }

    @GetMapping("/")
    public String index() {
        return "API DappSTP - Endpoints de Scraping disponibles en /api/scrape/...";
    }

    @GetMapping("/players")
    public String getPlayers() {
        System.out.println("➡️ Entró al endpoint /players");

        List<PlayerBarcelona> players = scraperService.scrapeAndSavePlayers();
    
        System.out.println("🧠 Scrap completado. Jugadores encontrados: " + players.size());
    
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

   
    @GetMapping("/hello")
     public String hello() {
       return "Hola desde el backend!";
    }
}


