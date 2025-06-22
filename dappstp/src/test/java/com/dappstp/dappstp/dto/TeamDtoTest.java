package com.dappstp.dappstp.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dappstp.dappstp.dto.footballData.TeamDto;

import static org.junit.jupiter.api.Assertions.*;

class TeamDtoTest {

    private TeamDto teamDto;

    @BeforeEach
    void setUp() {
        teamDto = new TeamDto();
    }

    @Test
    void testIdGetterAndSetter() {
        int testId = 101;
        teamDto.setId(testId);
        assertEquals(testId, teamDto.getId());
    }

    @Test
    void testNameGetterAndSetter() {
        String testName = "FC Barcelona";
        teamDto.setName(testName);
        assertEquals(testName, teamDto.getName());
    }

    @Test
    void testShortNameGetterAndSetter() {
        String testShortName = "Barça";
        teamDto.setShortName(testShortName);
        assertEquals(testShortName, teamDto.getShortName());
    }

    @Test
    void testTlaGetterAndSetter() {
        String testTla = "FCB";
        teamDto.setTla(testTla);
        assertEquals(testTla, teamDto.getTla());
    }

    @Test
    void testCrestGetterAndSetter() {
        String testCrestUrl = "https://example.com/crests/fcb.png";
        teamDto.setCrest(testCrestUrl);
        assertEquals(testCrestUrl, teamDto.getCrest());
    }

    @Test
    void testDefaultValues() {
        // For a newly instantiated DTO without setters called
        assertEquals(0, teamDto.getId(), "Default ID should be 0 for int");
        assertNull(teamDto.getName(), "Default name should be null");
        assertNull(teamDto.getShortName(), "Default shortName should be null");
        assertNull(teamDto.getTla(), "Default tla should be null");
        assertNull(teamDto.getCrest(), "Default crest should be null");
    }
}