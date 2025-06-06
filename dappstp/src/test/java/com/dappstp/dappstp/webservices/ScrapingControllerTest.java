package com.dappstp.dappstp.webservices;

import com.dappstp.dappstp.config.ApiPaths;
import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.security.JwtToken;
import com.dappstp.dappstp.service.UserService;
import com.dappstp.dappstp.service.scraping.clfinal.ScraperPlayersService;
import com.dappstp.dappstp.service.scraping.clfinal.SimpleScorePredictionScraperService;
import com.dappstp.dappstp.service.scraping.clfinal.TeamCharacteristicsScraperService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@WebMvcTest(
    controllers = ScrapingController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
class ScrapingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScraperPlayersService scraperServicePlayers;

    @MockBean
    private SimpleScorePredictionScraperService scorePredictionScraperService;

    @MockBean
    private TeamCharacteristicsScraperService teamCharacteristicsScraperService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtToken jwtToken;

    private final String baseScrapeUrl = ApiPaths.API_BASE + "/scrape";

    @Test
    void index_shouldReturnOkAndStatusMessage() throws Exception {
        mockMvc.perform(get(baseScrapeUrl + "/status"))
            .andExpect(status().isOk())
            .andExpect(content().string("API DappSTP - Endpoints de Scraping disponibles."));
    }

    @Test
    void getPlayers_shouldReturnOkAndPlayersString() throws Exception {
        Players player = new Players();
        player.setName("Test Player");
        player.setMatches("10");
        player.setGoals(5);
        player.setAssists(2);
        player.setRating(7.5);
        List<Players> mockPlayersList = Collections.singletonList(player);

        when(scraperServicePlayers.scrapeAndSavePlayers()).thenReturn(mockPlayersList);

        mockMvc.perform(get(baseScrapeUrl + "/players"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Test Player - 10 - 5 - 2 - 7.5")));
    }

    @Test
    void getPlayers_shouldReturnOkAndEmptyMessage_whenNoPlayersScraped() throws Exception {
        when(scraperServicePlayers.scrapeAndSavePlayers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get(baseScrapeUrl + "/players"))
            .andExpect(status().isOk())
            .andExpect(content().string("Jugadores:\n"));
    }

    @Test
    void prediction_shouldReturnOkAndPredictionString() throws Exception {
        String mockPredictionResult = "Predicted Score: Team A 2 - 1 Team B";
        when(scorePredictionScraperService.scrapeScorePrediction(anyString())).thenReturn(mockPredictionResult);

        mockMvc.perform(get(baseScrapeUrl + "/prediction"))
            .andExpect(status().isOk())
            .andExpect(content().string(mockPredictionResult));
    }

    @Test
    void prediction_shouldReturnInternalServerError_whenServiceThrowsException() throws Exception {
        when(scorePredictionScraperService.scrapeScorePrediction(anyString())).thenThrow(new RuntimeException("Scraping prediction failed"));

        mockMvc.perform(get(baseScrapeUrl + "/prediction"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("Error al obtener la predicción del resultado: Scraping prediction failed"));
    }

    @Test
    void getTeamCharacteristics_shouldReturnOkAndCharacteristicsList() throws Exception {
        List<String> mockCharacteristics = Arrays.asList("Strong Attack", "Good Defense");
        when(teamCharacteristicsScraperService.scrapeTeamCharacteristics(anyString())).thenReturn(mockCharacteristics);

        mockMvc.perform(get(baseScrapeUrl + "/teamcharacteristics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0]", is("Strong Attack")))
            .andExpect(jsonPath("$[1]", is("Good Defense")));
    }

    @Test
    void getTeamCharacteristics_shouldReturnInternalServerError_whenServiceThrowsException() throws Exception {
        when(teamCharacteristicsScraperService.scrapeTeamCharacteristics(anyString())).thenThrow(new RuntimeException("Scraping characteristics failed"));

        mockMvc.perform(get(baseScrapeUrl + "/teamcharacteristics"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$[0]", is("Error al obtener características: Scraping characteristics failed")));
            // La respuesta actual es una lista con un string de error, ajusta si es un ErrorResponse DTO
    }

    @Test
    void hello_shouldReturnOkAndHelloMessage() throws Exception {
        mockMvc.perform(get(baseScrapeUrl + "/hello"))
            .andExpect(status().isOk())
            .andExpect(content().string("Hola desde el backend!"));
    }
}