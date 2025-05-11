package com.dappstp.dappstp.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dappstp.dappstp.factory.UpcomingMatchFactory;
import com.dappstp.dappstp.model.Team;
import com.dappstp.dappstp.model.UpcomingMatch;
import com.dappstp.dappstp.repository.TeamRepository;
import com.dappstp.dappstp.service.impl.MatchServiceImpl;
import com.dappstp.dappstp.service.scraping.ScraperServiceMatches;

@ExtendWith(MockitoExtension.class)
public class UpcomingMatchServiceTest {
    
    @Mock
    private ScraperServiceMatches scraper;

    @Mock
    private TeamRepository teamRepo;

    @InjectMocks
    private MatchServiceImpl matchService;

    private Team arsenal;

    @BeforeEach
    void setUpMocks() {
        arsenal = new Team();
        arsenal.setId(1L);
        arsenal.setWhoscoredUrl("https://www.whoscored.com/Teams/13");
    }
/**PINCHA
 * ¨**    @Test
    void givenValidTeamId_whenGetAndPersistUpcomingMatches_thenReturnsMatches() {
        List<UpcomingMatch> mockMatches = List.of(
            UpcomingMatchFactory.createMatchArsenalAndChelsea()
        );

        when(teamRepo.findById(1L)).thenReturn(Optional.of(arsenal));
        when(scraper.scrapeAndSync(1L, "https://www.whoscored.com/Teams/13")).thenReturn(mockMatches);

        List<UpcomingMatch> result = matchService.getAndPersistUpcomingMatches(1L);
        assertEquals(1, result.size());
    }
 * 
 * 
 */


    @Test
    void givenInvalidTeamId_whenGetAndPersistUpcomingMatches_thenThrowsException() {
        when(teamRepo.findById(999L)).thenReturn(Optional.empty());

        try {
            matchService.getAndPersistUpcomingMatches(999L);
        } catch (Exception e) {
            assertEquals("Team no existe", e.getMessage());
        }
    }
}
