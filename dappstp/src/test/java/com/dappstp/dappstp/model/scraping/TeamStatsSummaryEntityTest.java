package com.dappstp.dappstp.model.scraping;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeamStatsSummaryEntityTest {

    @Test
    void testNoArgsConstructor() {
        TeamStatsSummaryEntity summary = new TeamStatsSummaryEntity();
        assertNotNull(summary);
        assertNotNull(summary.getStats());
        assertTrue(summary.getStats().isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        TeamStatsSummaryEntity summary = new TeamStatsSummaryEntity();

        Long id = 1L;
        String homeTeamName = "Home Team";
        String awayTeamName = "Away Team";
        String homeTeamEmblemUrl = "home.png";
        String homeMatchesPlayed = "10";
        String awayTeamEmblemUrl = "away.png";
        String awayMatchesPlayed = "12";

        summary.setId(id);
        summary.setHomeTeamName(homeTeamName);
        summary.setAwayTeamName(awayTeamName);
        summary.setHomeTeamEmblemUrl(homeTeamEmblemUrl);
        summary.setHomeMatchesPlayed(homeMatchesPlayed);
        summary.setAwayTeamEmblemUrl(awayTeamEmblemUrl);
        summary.setAwayMatchesPlayed(awayMatchesPlayed);

        assertEquals(id, summary.getId());
        assertEquals(homeTeamName, summary.getHomeTeamName());
        assertEquals(awayTeamName, summary.getAwayTeamName());
        assertEquals(homeTeamEmblemUrl, summary.getHomeTeamEmblemUrl());
        assertEquals(homeMatchesPlayed, summary.getHomeMatchesPlayed());
        assertEquals(awayTeamEmblemUrl, summary.getAwayTeamEmblemUrl());
        assertEquals(awayMatchesPlayed, summary.getAwayMatchesPlayed());
    }

    @Test
    void testAddStatDetail() {
        TeamStatsSummaryEntity summary = new TeamStatsSummaryEntity();
        StatDetailEntity statDetail = new StatDetailEntity("Goals", "2", "1");

        summary.addStatDetail(statDetail);

        assertTrue(summary.getStats().contains(statDetail));
        assertEquals(1, summary.getStats().size());
        assertEquals(summary, statDetail.getSummary());
    }

    @Test
    void testRemoveStatDetail() {
        TeamStatsSummaryEntity summary = new TeamStatsSummaryEntity();
        StatDetailEntity statDetail1 = new StatDetailEntity("Goals", "2", "1");
        StatDetailEntity statDetail2 = new StatDetailEntity("Shots", "10", "5");

        summary.addStatDetail(statDetail1);
        summary.addStatDetail(statDetail2);

        assertEquals(2, summary.getStats().size());

        summary.removeStatDetail(statDetail1);

        assertFalse(summary.getStats().contains(statDetail1));
        assertTrue(summary.getStats().contains(statDetail2));
        assertEquals(1, summary.getStats().size());
        assertNull(statDetail1.getSummary());
        assertEquals(summary, statDetail2.getSummary());
    }

    @Test
    void testEqualsAndHashCode() {
        // Lombok's @Data generates equals and hashCode.
        // This test is a basic check.
        TeamStatsSummaryEntity summary1 = new TeamStatsSummaryEntity();
        summary1.setId(1L);
        summary1.setHomeTeamName("Team A");

        TeamStatsSummaryEntity summary2 = new TeamStatsSummaryEntity();
        summary2.setId(1L);
        summary2.setHomeTeamName("Team A");

        TeamStatsSummaryEntity summary3 = new TeamStatsSummaryEntity();
        summary3.setId(2L);
        summary3.setHomeTeamName("Team B");

        assertEquals(summary1, summary2);
        assertNotEquals(summary1, summary3);
        assertEquals(summary1.hashCode(), summary2.hashCode());
        assertNotEquals(summary1.hashCode(), summary3.hashCode());
    }
}