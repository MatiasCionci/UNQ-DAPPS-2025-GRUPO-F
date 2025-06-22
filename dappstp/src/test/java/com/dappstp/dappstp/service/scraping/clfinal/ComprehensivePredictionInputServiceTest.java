package com.dappstp.dappstp.service.scraping.clfinal;

import com.dappstp.dappstp.dto.championsLeague.TeamStatsSummaryDto;
import com.dappstp.dappstp.dto.footballData.MatchesApiResponseDto;
import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.model.Prediction;
import com.dappstp.dappstp.model.queryhistory.PredictionLog;
import com.dappstp.dappstp.repository.PredictionLogRepository;
import com.dappstp.dappstp.service.PlayersService;
import com.dappstp.dappstp.service.getapifootball.FootballApiService;
import com.dappstp.dappstp.service.predictionia.PredictionService;
import com.dappstp.dappstp.service.scraping.championsLeagueFinal.CLFinalTeamStatsSummaryScraperService;
import com.dappstp.dappstp.service.scraping.championsLeagueFinal.ComprehensivePredictionInputService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComprehensivePredictionInputServiceTest {

    @Mock
    private FootballApiService mockFootballApiService;
    @Mock
    private PlayersService mockPlayersService;
    @Mock
    private CLFinalTeamStatsSummaryScraperService mockClFinalScraperService;
    @Mock
    private PredictionService mockPredictionService;
    @Mock
    private PredictionLogRepository mockPredictionLogRepository;
    @Mock
    private ObjectMapper mockObjectMapper;

    @InjectMocks
    private ComprehensivePredictionInputService service;

    @Test
    void aggregateDataForPrediction_success() {
        MatchesApiResponseDto interMatchesDto = new MatchesApiResponseDto(); // Populate if needed
        MatchesApiResponseDto psgMatchesDto = new MatchesApiResponseDto();   // Populate if needed
        Players player = new Players();
        player.setName("Test Player");
        player.setMatches("10");
        player.setGoals(5);
        player.setAssists(2);
        player.setRating(7.5);
        List<Players> playersList = Collections.singletonList(player);
        TeamStatsSummaryDto clFinalStatsDto = new TeamStatsSummaryDto(); // Populate if needed

        when(mockFootballApiService.getMatches("108")).thenReturn(interMatchesDto);
        when(mockFootballApiService.getMatches("524")).thenReturn(psgMatchesDto);
        when(mockPlayersService.findAllPlayers()).thenReturn(playersList);
        when(mockClFinalScraperService.retrieveLatestTeamStatsSummaryFromDB()).thenReturn(clFinalStatsDto);

        String result = service.aggregateDataForPrediction();

        assertNotNull(result);
        assertTrue(result.contains("INTER_MATCHES_START"));
        assertTrue(result.contains(interMatchesDto.toString()));
        assertTrue(result.contains("PSG_MATCHES_START"));
        assertTrue(result.contains(psgMatchesDto.toString()));
        assertTrue(result.contains("ALL_PLAYERS_START"));
        assertTrue(result.contains("Jugador{nombre=Test Player, partidos=10, goles=5, asistencias=2, rating=7.5}"));
        assertTrue(result.contains("CL_FINAL_STATS_START"));
        assertTrue(result.contains(clFinalStatsDto.toString()));

        verify(mockFootballApiService).getMatches("108");
        verify(mockFootballApiService).getMatches("524");
        verify(mockPlayersService).findAllPlayers();
        verify(mockClFinalScraperService).retrieveLatestTeamStatsSummaryFromDB();
    }

    @Test
    void aggregateDataForPrediction_someDataUnavailable() {
        when(mockFootballApiService.getMatches("108")).thenReturn(null); // Inter data unavailable
        when(mockPlayersService.findAllPlayers()).thenReturn(Collections.emptyList()); // No players

        String result = service.aggregateDataForPrediction();

        assertTrue(result.contains("DATOS_INTER_NO_DISPONIBLES"));
        assertTrue(result.contains("JUGADORES_NO_DISPONIBLES"));
        assertTrue(result.contains("STATS_CL_FINAL_NO_DISPONIBLES")); // Assuming clFinalScraperService returns null
    }


    @Test
    void generateAndLogComprehensivePrediction_success() throws Exception {
        String aggregatedData = "Aggregated Data Sample";
        Prediction mockPrediction = new Prediction(); // Populate if needed
        String predictionJson = "{\"prediction\":\"sample\"}";

        // Mock the aggregateDataForPrediction or let it run if simple enough (as in previous test)
        // For this test, let's assume aggregateDataForPrediction works and focus on logging
        ComprehensivePredictionInputService spyService = spy(service);
        doReturn(aggregatedData).when(spyService).aggregateDataForPrediction();

        when(mockPredictionService.analyzeMatch(aggregatedData)).thenReturn(mockPrediction);
        when(mockObjectMapper.writeValueAsString(mockPrediction)).thenReturn(predictionJson);
        when(mockPredictionLogRepository.save(any(PredictionLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Prediction resultPrediction = spyService.generateAndLogComprehensivePrediction();

        assertEquals(mockPrediction, resultPrediction);
        verify(mockPredictionService).analyzeMatch(aggregatedData);
        verify(mockObjectMapper).writeValueAsString(mockPrediction);
        ArgumentCaptor<PredictionLog> logCaptor = ArgumentCaptor.forClass(PredictionLog.class);
        verify(mockPredictionLogRepository).save(logCaptor.capture());

        PredictionLog savedLog = logCaptor.getValue();
        assertEquals(aggregatedData, savedLog.getRequestData());
        assertEquals(predictionJson, savedLog.getPredictionResult());
        assertEquals("COMPREHENSIVE", savedLog.getPredictionType());
    }

    @Test
    void getPredictionHistory_differentDays_success() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<PredictionLog> expectedLogs = Collections.singletonList(new PredictionLog());

        // When start and end are on different days, the service uses the 'end' date as is.
        when(mockPredictionLogRepository.findByCreatedAtBetween(start, end)).thenReturn(expectedLogs);

        List<PredictionLog> actualLogs = service.getPredictionHistory(start, end);

        assertEquals(expectedLogs, actualLogs);
        verify(mockPredictionLogRepository).findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getPredictionHistory_sameDay_endDateAdjusted_success() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.withHour(10); // Start of the day
        LocalDateTime end = now.withHour(15);   // Same day, different time
        List<PredictionLog> expectedLogs = Collections.singletonList(new PredictionLog());

        // When start and end are on the same day, the service adjusts 'end' to the end of the day.
        LocalDateTime expectedEndDate = end.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        when(mockPredictionLogRepository.findByCreatedAtBetween(start, expectedEndDate)).thenReturn(expectedLogs);

        List<PredictionLog> actualLogs = service.getPredictionHistory(start, end);

        assertEquals(expectedLogs, actualLogs);
        verify(mockPredictionLogRepository).findByCreatedAtBetween(start, expectedEndDate);
    }
}
