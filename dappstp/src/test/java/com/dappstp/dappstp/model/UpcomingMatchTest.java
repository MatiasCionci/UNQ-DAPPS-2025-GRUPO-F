package com.dappstp.dappstp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.dappstp.dappstp.factory.UpcomingMatchFactory;

public class UpcomingMatchTest {
    
    @Test
    void testCreateMatchArsenalAndChelsea() {

        UpcomingMatch match = UpcomingMatchFactory.createMatchArsenalAndChelsea();

        assertEquals("Arsenal", match.getHomeTeam(), "Home team should be Arsenal");
        assertEquals("Chelsea", match.getAwayTeam(), "Away team should be Chelsea");
        assertEquals("Emirates Stadium", match.getVenue(), "Venue should be Emirates Stadium");
        assertEquals("Premier League", match.getCompetition(), "Competition should be Premier League");
        assertEquals(LocalDateTime.of(2025, 5, 10, 16, 0), match.getKickoff(), "Kickoff date/time mismatch");
        assertEquals(MatchStatus.PENDING, match.getStatus(), "Status should be PENDING");
        assertEquals(1L, match.getTeamId(), "Team ID should be 1");
    }

    @Test
    void testCreateMatchManchesterAndLiverpool() {
        UpcomingMatch match = UpcomingMatchFactory.createMatchManchesterAndLiverpool();

        assertEquals("Manchester City", match.getHomeTeam(), "Home team should be Manchester City");
        assertEquals("Liverpool", match.getAwayTeam(), "Away team should be Liverpool");
        assertEquals("Emirates Stadium", match.getVenue(), "Venue should be Emirates Stadium");
        assertEquals("Premier League", match.getCompetition(), "Competition should be Premier League");
        assertEquals(LocalDateTime.of(2025, 6, 5, 17, 0), match.getKickoff(), "Kickoff date/time mismatch");
        assertEquals(MatchStatus.PENDING, match.getStatus(), "Status should be PENDING");
        assertEquals(2L, match.getTeamId(), "Team ID should be 2");
    }

    @Test
    void testMatchesNotEqual() {
        UpcomingMatch m1 = UpcomingMatchFactory.createMatchArsenalAndChelsea();
        UpcomingMatch m2 = UpcomingMatchFactory.createMatchManchesterAndLiverpool();
        assertNotEquals(m1.getHomeTeam(), m2.getHomeTeam(), "Home teams should differ");
        assertNotEquals(m1.getAwayTeam(), m2.getAwayTeam(), "Away teams should differ");
        assertNotEquals(m1.getKickoff(), m2.getKickoff(), "Kickoff dates should differ");
    }

}
