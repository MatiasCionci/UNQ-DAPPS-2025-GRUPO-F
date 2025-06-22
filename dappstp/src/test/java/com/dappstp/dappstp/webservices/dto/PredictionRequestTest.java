package com.dappstp.dappstp.webservices.dto;

import org.junit.jupiter.api.Test;

import com.dappstp.dappstp.dto.webService.PredictionRequest;

import static org.junit.jupiter.api.Assertions.*;

class PredictionRequestTest {

    @Test
    void gettersAndSetters_shouldWorkCorrectly() {
        PredictionRequest request = new PredictionRequest();
        String matchId = "match123";
        String scrapedData = "Some scraped data for the match.";

        request.setMatchId(matchId);
        request.setScrapedData(scrapedData);

        assertEquals(matchId, request.getMatchId(), "MatchId should be set and retrieved correctly");
        assertEquals(scrapedData, request.getScrapedData(), "ScrapedData should be set and retrieved correctly");
    }

    @Test
    void defaultConstructor_shouldInitializeFieldsAsNull() {
        PredictionRequest request = new PredictionRequest();

        assertNull(request.getMatchId(), "MatchId should be null initially");
        assertNull(request.getScrapedData(), "ScrapedData should be null initially");
    }

    // Si tuvieras un constructor con argumentos, lo probarías así:
    // @Test
    // void allArgsConstructor_shouldWorkCorrectly() {
    //     PredictionRequest request = new PredictionRequest("testId", "testData");
    //     assertEquals("testId", request.getMatchId());
    //     assertEquals("testData", request.getScrapedData());
    // }
}