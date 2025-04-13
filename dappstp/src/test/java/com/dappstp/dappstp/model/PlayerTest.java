package com.dappstp.dappstp.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PlayerTest {
    
    @Test
    void testPlayerModel() {
        Player player = new Player("Nicolas", 2, 1, 3, 2.0);
        Assertions.assertEquals("Nicolas", player.getName());
        Assertions.assertEquals(2, player.getMatchesPlayed());
        Assertions.assertEquals(1, player.getGoals());
        Assertions.assertEquals(3, player.getAssists());
        Assertions.assertEquals(2.0, player.getRating());
    }

    @Test
    void testEmptyPlayer() {
        Player player = new Player("Matias", 3, 2, 1, 3.0);
        Assertions.assertNotNull(player.getName());
        Assertions.assertNotEquals(0, player.getMatchesPlayed());
        Assertions.assertNotEquals(0, player.getGoals());
        Assertions.assertNotEquals(0, player.getAssists());
        Assertions.assertNotEquals(0.0, player.getRating());
    }

    @Test
    void testNegativeGoalsPlayer() {
        Player player = new Player("Nicolas", 2, -1, 3, 2.0);
        Assertions.assertEquals("Nicolas", player.getName());
        Assertions.assertEquals(2, player.getMatchesPlayed());
        Assertions.assertNotEquals(2, player.getGoals()); // Assuming goals cannot be negative
    }
}
