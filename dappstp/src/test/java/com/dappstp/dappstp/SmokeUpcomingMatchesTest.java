package com.dappstp.dappstp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.dappstp.dappstp.service.MatchService;
import com.dappstp.dappstp.service.TeamService;
import com.dappstp.dappstp.service.scraping.ScraperServiceMatches;
import com.dappstp.dappstp.webservices.MatchController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SmokeUpcomingMatchesTest {
    private MockMvc mvc;

    @Mock
    private ScraperServiceMatches scraper;

    @Mock
    private MatchService matchService;

    @Mock
    private TeamService teamService;

    @InjectMocks
    private MatchController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void endpointUpcomingMatchesExists() throws Exception {
        mvc.perform(get("/api/teams/1/upcoming-matches"))
           .andExpect(status().is2xxSuccessful());
    }
}
