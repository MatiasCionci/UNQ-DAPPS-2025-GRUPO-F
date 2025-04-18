package com.dappstp.dappstp.WebService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dappstp.dappstp.model.Player;
import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.service.PlayerService;
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
public class ScrapingController {
@Autowired
private final PlayerService playerService;
@Autowired
private final ScraperServicePlayers scraperService;


    public ScrapingController(PlayerService playerService, ScraperServicePlayers scraperService) {
        this.playerService = playerService;
        this.scraperService = scraperService;
    }
    @GetMapping("/")
    public String index() {
        return "Hola, bienvenido a DappSTP!";
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

    @Operation(summary = "Obtener jugadores mockeados por equipo",
               description = "Devuelve una lista predefinida de jugadores filtrada por el nombre del equipo proporcionado en la URL.")
    @ApiResponse(responseCode = "200", description = "Lista de jugadores del equipo obtenida con éxito",
                 content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Player.class)))
    @ApiResponse(responseCode = "404", description = "Equipo no encontrado o sin jugadores") // Opcional: describir caso no encontrado
    @GetMapping("/players/by-team/{teamName}") // Ruta que incluye el nombre del equipo como variable
    public List<Player> getPlayersByTeam(
            @Parameter(description = "Nombre del equipo a buscar", example = "FC Barcelona") // Describe el parámetro para Swagger
            @PathVariable String teamName) { // Lee la variable {teamName} de la URL

        System.out.println("➡️ Entró al endpoint /players/by-team/" + teamName);
        List<Player> players = playerService.getPlayersByTeam(teamName); // Llama al nuevo método del servicio
        if (players.isEmpty()) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron jugadores para el equipo: " + teamName);
        }
        return players;
    }

    @GetMapping("/hello")
public String hello() {
    return "Hola desde el backend!";
}
}

