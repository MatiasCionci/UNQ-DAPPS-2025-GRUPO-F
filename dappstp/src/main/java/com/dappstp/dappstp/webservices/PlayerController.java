package com.dappstp.dappstp.webservices;
import org.springframework.web.bind.annotation.PathVariable; // Para leer variables de la ruta
import io.swagger.v3.oas.annotations.Parameter; // Para documentar parámetros

import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.model.queryhistory.PredictionLog;
import com.dappstp.dappstp.config.ApiPaths; // Asumiendo que tienes ApiPaths
import com.dappstp.dappstp.service.PlayersService; // Ensure PlayersService is imported
import com.dappstp.dappstp.service.scraping.championsLeagueFinal.ComprehensivePredictionInputService;

// Importa ComprehensivePredictionInputService
import lombok.extern.slf4j.Slf4j; // Para logging
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity; // Para una mejor respuesta HTTP
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController // Marca esta clase como un controlador REST
@RequestMapping(ApiPaths.API_BASE + "/playersEntity") // Usando ApiPaths
@Tag(name = "Players", description = "Endpoints para gestionar y consultar información de jugadores.")
@Slf4j // Habilita el logging fácil con Lombok
public class PlayerController {

     // private final PlayersRepository playerRepository; // Elimina esto
    private final PlayersService playerService; // Inyecta el servicio
    private final ComprehensivePredictionInputService comprehensivePredictionInputService; // Inyecta el servicio

    
    public PlayerController(PlayersService playerService, ComprehensivePredictionInputService comprehensivePredictionInputService) { // Modifica el constructor
        this.playerService = playerService;
        this.comprehensivePredictionInputService = comprehensivePredictionInputService;
    }

    @GetMapping
    @Operation(summary = "Obtener todos los jugadores",
               description = "Devuelve una lista de todos los jugadores almacenados en la base de datos.")
    @ApiResponse(responseCode = "200", description = "Lista de jugadores obtenida exitosamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Players.class))))
    public ResponseEntity<List<Players>> getAllPlayers() {
        log.info("➡️ Endpoint /api/playersEntity invocado para obtener todos los jugadores.");
        try {
            // Llama al método del servicio
            List<Players> players = playerService.findAllPlayers();

            // La lógica de logging sobre si se encontraron o no puede quedarse aquí
            // o moverse al servicio, como prefieras. El ejemplo la movió al servicio.

            // Devuelve la lista de jugadores con un estado HTTP 200 OK
            return ResponseEntity.ok(players);

        } catch (Exception e) {
            // El manejo de excepciones a nivel de controlador sigue siendo útil
            log.error("🚨 Error al intentar obtener jugadores a través del servicio.", e);
            return ResponseEntity.internalServerError().build();
        }
    }
     @GetMapping("/by-name/{playerName}")
    @Operation(summary = "Obtener jugadores por nombre",
               description = "Devuelve una lista de jugadores cuyo nombre contenga el texto proporcionado (ignora mayúsculas/minúsculas).")
    @ApiResponse(responseCode = "200", description = "Lista de jugadores encontrada exitosamente.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Players.class))))
    @ApiResponse(responseCode = "404", description = "No se encontraron jugadores con ese nombre.", content = @Content(mediaType = "application/json"))
    public ResponseEntity<List<Players>> getPlayersByName(
            @Parameter(description = "Nombre o parte del nombre del jugador a buscar.", required = true, example = "Messi")
            @PathVariable String playerName) {
        log.info("➡️ Endpoint /api/playersEntity/by-name/{} invocado.", playerName);
        try {
            List<Players> players = playerService.findPlayersByName(playerName);
            if (players.isEmpty()) {
                return ResponseEntity.notFound().build(); // O ResponseEntity.ok(players) para devolver lista vacía
            }
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            log.error("🚨 Error al intentar obtener jugadores por nombre '{}' a través del servicio.", playerName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/search-history")
    @Operation(summary = "Consultar historial de búsquedas de jugadores")
    public  ResponseEntity<List<PredictionLog>>  getPlayerSearchHistory(
    @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);
    // Suponiendo que tienes 'comprehensivePredictionInputService' inyectado y con el método getPlayerSearchHistory
        try {
        List<PredictionLog> history = comprehensivePredictionInputService.getPlayerSearchHistory(startOfDay, endOfDay);
        return ResponseEntity.ok(history);
         } catch (Exception e) {
        log.error("🚨 Error al obtener el historial de búsqueda de jugadores para la fecha {}: {}", date, e.getMessage(), e);
        // Aunque el tipo de retorno es ResponseEntity<List<PredictionLog>>, 
        // devolvemos un cuerpo de ErrorResponse en caso de error, lo cual es una práctica común.
        // El cliente deberá manejar la posibilidad de recibir un ErrorResponse.
        return ResponseEntity.internalServerError().body(null); // O considera un ErrorResponse DTO si el cliente lo espera
    }
}
}