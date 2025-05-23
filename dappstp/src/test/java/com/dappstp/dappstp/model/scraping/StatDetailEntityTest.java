package com.dappstp.dappstp.model.scraping;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatDetailEntityTest {

    @Test
    void testNoArgsConstructor() {
        StatDetailEntity statDetail = new StatDetailEntity();
        assertNotNull(statDetail);
    }

    @Test
    void testConstructorWithParameters() {
        String label = "Goals";
        String homeValue = "2";
        String awayValue = "1";
        StatDetailEntity statDetail = new StatDetailEntity(label, homeValue, awayValue);

        assertEquals(label, statDetail.getLabel());
        assertEquals(homeValue, statDetail.getHomeValue());
        assertEquals(awayValue, statDetail.getAwayValue());
    }

    @Test
    void testGettersAndSetters() {
        StatDetailEntity statDetail = new StatDetailEntity();

        Long id = 1L;
        String label = "Shots";
        String homeValue = "10";
        String awayValue = "5";
        TeamStatsSummaryEntity summary = new TeamStatsSummaryEntity();

        statDetail.setId(id);
        statDetail.setLabel(label);
        statDetail.setHomeValue(homeValue);
        statDetail.setAwayValue(awayValue);
        statDetail.setSummary(summary);

        assertEquals(id, statDetail.getId());
        assertEquals(label, statDetail.getLabel());
        assertEquals(homeValue, statDetail.getHomeValue());
        assertEquals(awayValue, statDetail.getAwayValue());
        assertEquals(summary, statDetail.getSummary());
    }

    @Test
    void testEqualsAndHashCode() {
        // Lombok's @Data generates equals and hashCode.
        // This test is a basic check. For more thorough testing, consider specific scenarios.
        StatDetailEntity statDetail1 = new StatDetailEntity("Possession", "60%", "40%");
        statDetail1.setId(1L);

        StatDetailEntity statDetail2 = new StatDetailEntity("Possession", "60%", "40%");
        statDetail2.setId(1L);

        StatDetailEntity statDetail3 = new StatDetailEntity("Fouls", "10", "12");
        statDetail3.setId(2L);

        assertEquals(statDetail1, statDetail2);
        assertNotEquals(statDetail1, statDetail3);
        assertEquals(statDetail1.hashCode(), statDetail2.hashCode());
        assertNotEquals(statDetail1.hashCode(), statDetail3.hashCode());
    }
}