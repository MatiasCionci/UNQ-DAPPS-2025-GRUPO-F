package com.dappstp.dappstp.service.scraping.clfinal.dto;

import org.junit.jupiter.api.Test;

import com.dappstp.dappstp.dto.championsLeague.StatDetailDto;
import com.dappstp.dappstp.dto.championsLeague.TeamStatsSummaryDto;

import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TeamStatsSummaryDtoTest {

    @Test
    void testNoArgsConstructor() {
        TeamStatsSummaryDto dto = new TeamStatsSummaryDto();
        assertNull(dto.getHomeTeamName(), "HomeTeamName should be null initially");
        assertNull(dto.getAwayTeamName(), "AwayTeamName should be null initially");
        assertNull(dto.getHomeTeamEmblemUrl(), "HomeTeamEmblemUrl should be null initially");
        assertNull(dto.getHomeMatchesPlayed(), "HomeMatchesPlayed should be null initially");
        assertNull(dto.getAwayTeamEmblemUrl(), "AwayTeamEmblemUrl should be null initially");
        assertNull(dto.getAwayMatchesPlayed(), "AwayMatchesPlayed should be null initially");
        assertNull(dto.getStats(), "Stats list should be null initially");
    }

    @Test
    void testSettersAndGetters() {
        TeamStatsSummaryDto dto = new TeamStatsSummaryDto();

        String homeTeamName = "Real Madrid";
        String awayTeamName = "Barcelona";
        String homeTeamEmblemUrl = "url_home_emblem";
        String homeMatchesPlayed = "10";
        String awayTeamEmblemUrl = "url_away_emblem";
        String awayMatchesPlayed = "12";
        List<StatDetailDto> stats = Collections.singletonList(new StatDetailDto("Goals", "3", "0"));

        dto.setHomeTeamName(homeTeamName);
        dto.setAwayTeamName(awayTeamName);
        dto.setHomeTeamEmblemUrl(homeTeamEmblemUrl);
        dto.setHomeMatchesPlayed(homeMatchesPlayed);
        dto.setAwayTeamEmblemUrl(awayTeamEmblemUrl);
        dto.setAwayMatchesPlayed(awayMatchesPlayed);
        dto.setStats(stats);

        assertEquals(homeTeamName, dto.getHomeTeamName());
        assertEquals(awayTeamName, dto.getAwayTeamName());
        assertEquals(homeTeamEmblemUrl, dto.getHomeTeamEmblemUrl());
        assertEquals(homeMatchesPlayed, dto.getHomeMatchesPlayed());
        assertEquals(awayTeamEmblemUrl, dto.getAwayTeamEmblemUrl());
        assertEquals(awayMatchesPlayed, dto.getAwayMatchesPlayed());
        assertEquals(stats, dto.getStats());
    }

    @Test
    void testEqualsAndHashCode() {
        List<StatDetailDto> stats1 = Collections.singletonList(new StatDetailDto("Goals", "2", "1"));
        List<StatDetailDto> stats2 = Collections.singletonList(new StatDetailDto("Goals", "2", "1"));
        List<StatDetailDto> stats3 = Collections.singletonList(new StatDetailDto("Shots", "10", "5"));

        TeamStatsSummaryDto dto1 = new TeamStatsSummaryDto();
        dto1.setHomeTeamName("Team A");
        dto1.setAwayTeamName("Team B");
        dto1.setHomeTeamEmblemUrl("urlA");
        dto1.setHomeMatchesPlayed("5");
        dto1.setAwayTeamEmblemUrl("urlB");
        dto1.setAwayMatchesPlayed("5");
        dto1.setStats(stats1);

        TeamStatsSummaryDto dto2 = new TeamStatsSummaryDto(); // Same as dto1
        dto2.setHomeTeamName("Team A");
        dto2.setAwayTeamName("Team B");
        dto2.setHomeTeamEmblemUrl("urlA");
        dto2.setHomeMatchesPlayed("5");
        dto2.setAwayTeamEmblemUrl("urlB");
        dto2.setAwayMatchesPlayed("5");
        dto2.setStats(stats2);

        TeamStatsSummaryDto dto3 = new TeamStatsSummaryDto(); // Different stats and names
        dto3.setHomeTeamName("Team C");
        dto3.setAwayTeamName("Team D");
        dto3.setHomeTeamEmblemUrl("urlC");
        dto3.setHomeMatchesPlayed("6");
        dto3.setAwayTeamEmblemUrl("urlD");
        dto3.setAwayMatchesPlayed("6");
        dto3.setStats(stats3);

        assertEquals(dto1, dto2, "dto1 and dto2 should be equal");
        assertNotEquals(dto1, dto3, "dto1 and dto3 should not be equal");

        assertEquals(dto1.hashCode(), dto2.hashCode(), "Hash codes for dto1 and dto2 should be equal");
        assertNotEquals(dto1.hashCode(), dto3.hashCode(), "Hash codes for dto1 and dto3 should not be equal");
    }

    @Test
    void testToString() {
        TeamStatsSummaryDto dto = new TeamStatsSummaryDto();
        dto.setHomeTeamName("Liverpool");
        String dtoAsString = dto.toString();
        assertTrue(dtoAsString.contains("homeTeamName=Liverpool"), "toString should contain homeTeamName");
    }
}