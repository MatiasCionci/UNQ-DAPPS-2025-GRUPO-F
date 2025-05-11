package com.dappstp.dappstp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.dappstp.dappstp.factory.TeamFactory;

public class TeamTest {
    @Test
    void givenFactoryTeam_whenGettersCalled_thenReturnsCorrectValues() {
        Team psg = TeamFactory.createPSG();

        assertEquals(304L, psg.getId());
        assertEquals("Paris Saint-Germain", psg.getName());
        assertEquals("https://es.whoscored.com/teams/304/fixtures/francia-paris-saint-germain", psg.getWhoscoredUrl());
    }

    @Test
    void givenFactoryTeam_whenComparingWithWrongValues_thenReturnsNotEquals() {
        Team psg = TeamFactory.createPSG();

        assertNotEquals(1L, psg.getId(), "ID should not match 1");
        assertNotEquals("Barcelona FC", psg.getName(), "Name should not match 'Barcelona FC'");
        assertNotEquals(
            "https://wrong.url/fixture",
            psg.getWhoscoredUrl(),
            "URL should not match wrong URL"
        );
    }

    @Test
    void givenFactoryArsenal_whenGettersCalled_thenReturnsCorrectValues() {
        Team arsenal = TeamFactory.createArsenal();

        assertEquals(1L, arsenal.getId(), "ID should be 1 for Arsenal");
        assertEquals("Arsenal", arsenal.getName(), "Name should be Arsenal");
        assertEquals(
            "https://www.whoscored.com/Teams/13",
            arsenal.getWhoscoredUrl(),
            "WhoScored URL should match for Arsenal"
        );
    }

}
