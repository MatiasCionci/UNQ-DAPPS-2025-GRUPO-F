package com.dappstp.dappstp.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void testEmptyPlayer() {
        Player player = new Player("Matias", "Barcelona", 2, 1, 3, 2.0);
        Assertions.assertNotNull(player.getName());
        Assertions.assertNotEquals(0, player.getMatchesPlayed());
        Assertions.assertNotEquals(0, player.getGoals());
        Assertions.assertNotEquals(0, player.getAssists());
        Assertions.assertNotEquals(0.0, player.getRating());
    }

    @Test
    void testNegativeGoalsPlayer() {
        Player player = new Player("Nicolas", "Manchester City", 2, -1, 3, 2.0);
        Assertions.assertEquals("Nicolas", player.getName());
        Assertions.assertEquals(2, player.getMatchesPlayed());
        Assertions.assertNotEquals(2, player.getGoals()); // Assuming goals cannot be negative
    }
}
