package com.dappstp.dappstp.WebService; // Asegúrate que el paquete sea correcto para tu estructura

import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.repository.PlayerBarcelonaRepository;
import lombok.extern.slf4j.Slf4j; // Para logging
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // Para una mejor respuesta HTTP
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController // Marca esta clase como un controlador REST
@RequestMapping("/api/playersEntity") // Define la ruta base para este controlador
@Slf4j // Habilita el logging fácil con Lombok
public class PlayerController {

    private final PlayerBarcelonaRepository playerRepository;

    // Inyecta el repositorio usando el constructor (buena práctica)
    @Autowired
    public PlayerController(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // Método para manejar peticiones GET a /api/players
    @GetMapping
    public ResponseEntity<List<PlayerBarcelona>> getAllBarcelonaPlayers() {
        log.info("➡️ Endpoint /api/players invocado para obtener todos los jugadores del Barcelona.");
        try {
            // Usa el método findAll() del repositorio JpaRepository
            List<PlayerBarcelona> players = playerRepository.findAll();

            if (players.isEmpty()) {
                log.info("🟡 No se encontraron jugadores en la base de datos.");
            } else {
                log.info("✅ Encontrados {} jugadores en la base de datos.", players.size());
            }

            // Devuelve la lista de jugadores con un estado HTTP 200 OK
            // Spring Boot convertirá automáticamente la lista a JSON
            return ResponseEntity.ok(players);

        } catch (Exception e) {
            log.error("🚨 Error al intentar obtener jugadores de la base de datos.", e);
            // En caso de error, devuelve un estado HTTP 500 Internal Server Error
            return ResponseEntity.internalServerError().build();
        }
    }
}
