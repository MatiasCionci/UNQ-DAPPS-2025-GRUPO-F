package webservices;

import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.service.PlayersService; // Ensure PlayersService is imported
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

     // private final PlayersRepository playerRepository; // Elimina esto
    private final PlayersService playerService; // Inyecta el servicio

    @Autowired
    public PlayerController(PlayersService playerService) { // Modifica el constructor
        this.playerService = playerService;
    }

    @GetMapping
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
}