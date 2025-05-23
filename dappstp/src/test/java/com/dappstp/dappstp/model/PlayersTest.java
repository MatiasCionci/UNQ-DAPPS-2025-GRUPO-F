package com.dappstp.dappstp.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayersTest {

    @Test
    void testNoArgsConstructor() {
        Players player = new Players();
        assertNotNull(player);
        assertNull(player.getId()); // ID is null initially
    }

    @Test
    void testConstructorWithParameters() {
        String name = "Lionel Messi";
        String matches = "30(2)";
        int goals = 25;
        int assists = 15;
        double rating = 9.5;

        Players player = new Players(name, matches, goals, assists, rating);

        assertNull(player.getId()); // ID is not set by this constructor
        assertEquals(name, player.getName());
        assertEquals(matches, player.getMatches());
        assertEquals(goals, player.getGoals());
        assertEquals(assists, player.getAssists());
        assertEquals(rating, player.getRating());
    }

    @Test
    void testGettersAndSetters() {
        Players player = new Players();

        player.setId(1L);
        player.setName("Cristiano Ronaldo");
        player.setMatches("28(0)");
        player.setGoals(30);
        player.setAssists(10);
        player.setRating(9.2);

        assertEquals(1L, player.getId());
        assertEquals("Cristiano Ronaldo", player.getName());
        assertEquals("28(0)", player.getMatches());
        assertEquals(30, player.getGoals());
        assertEquals(10, player.getAssists());
        assertEquals(9.2, player.getRating());
    }

    @Test
    void testEqualsAndHashCode() {
        // Lombok @Data should handle this.
        Players player1 = new Players("Neymar Jr", "25(5)", 20, 12, 8.8);
        player1.setId(10L);

        Players player2 = new Players("Neymar Jr", "25(5)", 20, 12, 8.8);
        player2.setId(10L);

        Players player3 = new Players("Kylian Mbappé", "32(1)", 28, 10, 9.0);
        player3.setId(11L);

        assertEquals(player1, player2);
        assertEquals(player1.hashCode(), player2.hashCode());
        assertNotEquals(player1, player3);
    }
}
