package com.dappstp.dappstp.service;
import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.repository.PlayersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service; // Importa @Service
import com.dappstp.dappstp.model.queryhistory.PredictionLog; // Importar PredictionLog
import com.dappstp.dappstp.repository.PredictionLogRepository; // Importar PredictionLogRepository
import java.util.stream.Collectors; // Para formatear la lista de jugadores encontrados

import java.util.List;

@Service // Marca esta clase como un servicio de Spring
@Slf4j
public class PlayersService {
    private final PlayersRepository playerRepository;
    private final PredictionLogRepository predictionLogRepository; // Añadir el repositorio de logs

    public PlayersService(PlayersRepository playerRepository,
                          PredictionLogRepository predictionLogRepository) {
        // Inyecta el repositorio de jugadores y el repositorio de logs
        this.predictionLogRepository = predictionLogRepository; // Inyecta el repositorio de logs
        this.playerRepository = playerRepository;
    }

    // Método que encapsula la lógica de obtener todos los jugadores
    public List<Players> findAllPlayers() {
        log.info("🔄 Consultando todos los jugadores desde el repositorio.");
        List<Players> players = playerRepository.findAll();
        if (players.isEmpty()) {
            log.info("🟡 No se encontraron jugadores en la base de datos (desde el servicio).");
        } else {
            log.info("✅ Encontrados {} jugadores en la base de datos (desde el servicio).", players.size());
        }
        return players;
        // Considera si quieres manejar excepciones aquí o dejarlas subir al controlador
    }

     // Nuevo método para buscar jugadores por nombre
    public List<Players> findPlayersByName(String name) {
        log.info("🔄 Consultando jugadores por nombre '{}' desde el repositorio.", name);
        List<Players> players = playerRepository.findByNameContainingIgnoreCase(name);
        String searchResultSummary;

        if (players.isEmpty()) {
            log.info("🟡 No se encontraron jugadores con el nombre '{}' (desde el servicio).", name);
            searchResultSummary = String.format("No se encontraron jugadores con el nombre '%s'.", name);

        } else {
            log.info("✅ Encontrados {} jugadores con el nombre '{}' (desde el servicio).", players.size(), name);
             // Crear un resumen de los jugadores encontrados para el log
            searchResultSummary = String.format("Encontrados %d jugadores: [%s]",
            players.size(),
            players.stream().map(Players::getName).collect(Collectors.joining(", ")));
        }
                // Guardar el log de la búsqueda del jugador
        try {
            PredictionLog searchLog = new PredictionLog(
                name, // requestData: el término de búsqueda
                searchResultSummary, // predictionResult: resumen de lo encontrado
                "PLAYER_SEARCH" // predictionType: un nuevo tipo para identificar estas búsquedas
            );
            predictionLogRepository.save(searchLog);
            log.info("📝 Historial de búsqueda de jugador para '{}' guardado con ID de log: {}", name, searchLog.getId());
        } catch (Exception e) {
            log.error("🚨 Error al guardar el historial de búsqueda de jugador para '{}': {}", name, e.getMessage(), e);
            // Se decide solo loguear el error para no interrumpir la funcionalidad principal de búsqueda.
        }

        return players;
    }
}

