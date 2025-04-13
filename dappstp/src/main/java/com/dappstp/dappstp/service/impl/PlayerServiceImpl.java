package com.dappstp.dappstp.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.dappstp.dappstp.model.Player;
import com.dappstp.dappstp.service.PlayerService;

public class PlayerServiceImpl implements PlayerService {

    private final List<Player> allPlayers;

    public PlayerServiceImpl() {
        // Datos mockeados para simular una base de datos o scraping
        allPlayers = new ArrayList<>();
        allPlayers.add(new Player("Lionel Messi", 30, 25, 18, 8.9, "Barcelona"));
        allPlayers.add(new Player("Xavi Hernández", 28, 3, 12, 7.4, "Barcelona"));
        allPlayers.add(new Player("Cristiano Ronaldo", 31, 27, 9, 8.7, "Real Madrid"));
        allPlayers.add(new Player("Luka Modric", 29, 4, 10, 7.8, "Real Madrid"));
        allPlayers.add(new Player("Erling Haaland", 26, 24, 3, 8.3, "Manchester City"));
    }

    @Override
    public List<Player> getPlayersByTeam(String teamName) {
        return allPlayers.stream()
                .filter(player -> teamName.equalsIgnoreCase(player.getTeamName()))
                .collect(Collectors.toList());
    }
}
