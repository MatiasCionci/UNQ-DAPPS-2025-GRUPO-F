package com.dappstp.dappstp.service;
import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.repository.PlayersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service; // Importa @Service

import java.util.List;

@Service // Marca esta clase como un servicio de Spring
@Slf4j
public class PlayersService {
    private final PlayersRepository playerRepository;

    @Autowired
    public PlayersService(PlayersRepository playerRepository) {
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

    // Aquí podrías añadir más métodos relacionados con la lógica de negocio de Players
    // ej: findPlayerById(Long id), savePlayer(Player player), etc.
}

