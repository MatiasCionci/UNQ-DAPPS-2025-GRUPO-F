package com.dappstp.dappstp.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dappstp.dappstp.dto.MatchDto;
import com.dappstp.dappstp.dto.TeamDto;

import static org.junit.jupiter.api.Assertions.*;

class MatchDtoTest {

    private MatchDto matchDto;
    private TeamDto homeTeam;
    private TeamDto awayTeam;

    @BeforeEach
    void setUp() {
        matchDto = new MatchDto();

        homeTeam = new TeamDto();
        homeTeam.setId(1);
        homeTeam.setName("Home Team FC");

        awayTeam = new TeamDto();
        awayTeam.setId(2);
        awayTeam.setName("Away Team Rovers");
    }

    @Test
    void testIdGetterAndSetter() {
        int testId = 12345;
        matchDto.setId(testId);
        assertEquals(testId, matchDto.getId());
    }

    @Test
    void testUtcDateGetterAndSetter() {
        String testDate = "2023-10-27T18:00:00Z";
        matchDto.setUtcDate(testDate);
        assertEquals(testDate, matchDto.getUtcDate());
    }

    @Test
    void testStatusGetterAndSetter() {
        String testStatus = "FINISHED";
        matchDto.setStatus(testStatus);
        assertEquals(testStatus, matchDto.getStatus());
    }

    @Test
    void testHomeTeamGetterAndSetter() {
        matchDto.setHomeTeam(homeTeam);
        assertEquals(homeTeam, matchDto.getHomeTeam());
        assertEquals("Home Team FC", matchDto.getHomeTeam().getName());
    }

    @Test
    void testAwayTeamGetterAndSetter() {
        matchDto.setAwayTeam(awayTeam);
        assertEquals(awayTeam, matchDto.getAwayTeam());
        assertEquals("Away Team Rovers", matchDto.getAwayTeam().getName());
    }

    @Test
    void testToString() {
        matchDto.setId(789);
        matchDto.setHomeTeam(homeTeam);
        matchDto.setAwayTeam(awayTeam);
        matchDto.setStatus("SCHEDULED");
        matchDto.setUtcDate("2024-01-15T20:00:00Z");

        String expectedString = "MatchDto{id=789, homeTeam=Home Team FC, awayTeam=Away Team Rovers, status='SCHEDULED', utcDate='2024-01-15T20:00:00Z'}";
        assertEquals(expectedString, matchDto.toString());
    }

    @Test
    void testToStringWithNullTeams() {
        matchDto.setId(101);
        matchDto.setHomeTeam(null);
        matchDto.setAwayTeam(null);
        matchDto.setStatus("POSTPONED");
        matchDto.setUtcDate("2024-02-01T12:00:00Z");

        String expectedString = "MatchDto{id=101, homeTeam=N/A, awayTeam=N/A, status='POSTPONED', utcDate='2024-02-01T12:00:00Z'}";
        assertEquals(expectedString, matchDto.toString());
    }
}