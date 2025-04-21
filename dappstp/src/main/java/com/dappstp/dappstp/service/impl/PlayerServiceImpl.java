package com.dappstp.dappstp.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; 

import org.springframework.stereotype.Service; 

import com.dappstp.dappstp.model.Player;
import com.dappstp.dappstp.service.PlayerService;

@Service 
public class PlayerServiceImpl implements PlayerService {

    private final List<Player> allPlayers;

    public PlayerServiceImpl() {
        // Datos mockeados actualizados con equipos
        allPlayers = new ArrayList<>();
        allPlayers.add(new Player("Lionel Messi", "Inter Miami CF", 30, 25, 18, 8.9));
        allPlayers.add(new Player("Xavi Hernández", "FC Barcelona", 28, 3, 12, 7.4)); // Nota: Xavi está retirado, ejemplo didáctico
        allPlayers.add(new Player("Cristiano Ronaldo", "Al Nassr", 31, 27, 9, 8.7));
        allPlayers.add(new Player("Luka Modric", "Real Madrid", 29, 4, 10, 7.8));
        allPlayers.add(new Player("Erling Haaland", "Manchester City", 26, 24, 3, 8.3));
        allPlayers.add(new Player("Pedri", "FC Barcelona", 25, 5, 6, 7.9)); // Añadido otro del Barça para probar filtro
    }

    public PlayerServiceImpl(List<Player> players) {
        this.allPlayers = players;
    }

    @Override 
    public List<Player> getPlayersByTeam(String teamName) {
        if (teamName == null || teamName.trim().isEmpty()) {
            return new ArrayList<>(); // Devuelve lista vacía si el nombre del equipo es inválido
        }    
        // Usamos streams para filtrar la lista
        return allPlayers.stream() // Convierte la lista en un stream
                .filter(player -> teamName.equalsIgnoreCase(player.getTeam())) // Filtra: se queda con los jugadores cuyo equipo coincide
                .collect(Collectors.toList()); // Recolecta los resultados en una nueva lista
    }
}

