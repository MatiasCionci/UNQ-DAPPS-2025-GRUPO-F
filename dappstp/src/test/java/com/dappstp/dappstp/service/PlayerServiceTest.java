package com.dappstp.dappstp.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dappstp.dappstp.model.Player;
import java.util.List;

public class PlayerServiceTest {
    
    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerServiceImpl();
    }

    @Test
    void shouldReturnPlayersFromGivenTeam() {

        String teamName = "Barcelona";
        List<Player> result = playerService.getPlayersByTeam(teamName);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(player -> teamName.equals(player.getTeamName())));
    }

    @Test
    void shouldReturnEmptyListIfNoPlayersFromTeam() {

        String teamName = "EquipoInexistente";
        List<Player> result = playerService.getPlayersByTeam(teamName);

        assertTrue(result.isEmpty());
    }
}
