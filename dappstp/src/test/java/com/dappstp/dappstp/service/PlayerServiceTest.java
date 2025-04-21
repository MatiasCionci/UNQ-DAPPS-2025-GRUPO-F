package com.dappstp.dappstp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import org.junit.jupiter.api.Test;

import com.dappstp.dappstp.factory.PlayerFactory;
import com.dappstp.dappstp.model.Player;
import com.dappstp.dappstp.service.impl.PlayerServiceImpl; 

public class PlayerServiceTest {
    
    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        List<Player> jugadores = List.of(
            PlayerFactory.crearJugadorTop(),           // Messi - Inter Miami
            PlayerFactory.crearJugadorPromesa(),       // Lamine - Barcelona
            PlayerFactory.crearJugadorConEstadisticasBajas() // Ortega - Manchester City
        );

        playerService = new PlayerServiceImpl(jugadores);
    }

    @Test
    void givenExistingTeam_whenGetPlayersByTeam_thenReturnsPlayersFromThatTeam() {
        List<Player> result = playerService.getPlayersByTeam("Barcelona");
        assertEquals(1, result.size());
    }

    @Test
    void givenNonexistentTeam_whenGetPlayersByTeam_thenReturnsEmptyList() {
        List<Player> result = playerService.getPlayersByTeam("Real Madrid");
        assertEquals(0, result.size());
    }

    @Test
    void givenNullTeamName_whenGetPlayersByTeam_thenReturnsEmptyList() {
        List<Player> result = playerService.getPlayersByTeam(null);
        assertEquals(0, result.size());
    }

    @Test
    void givenEmptyTeamName_whenGetPlayersByTeam_thenReturnsEmptyList() {
        List<Player> result = playerService.getPlayersByTeam("  ");
        assertEquals(0, result.size());
    }

    @Test
    void givenTeamNameWithDifferentCase_whenGetPlayersByTeam_thenIgnoresCase() {
        List<Player> result = playerService.getPlayersByTeam("inter miami");
        assertEquals(1, result.size());
    }

}
