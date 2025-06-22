package com.dappstp.dappstp.webservices;

import com.dappstp.dappstp.config.ApiPaths;
import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.model.queryhistory.PredictionLog;
import com.dappstp.dappstp.security.JwtToken;
import com.dappstp.dappstp.service.PlayersService;
import com.dappstp.dappstp.service.UserService;
import com.dappstp.dappstp.service.scraping.championsLeagueFinal.ComprehensivePredictionInputService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = PlayerController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false) // Disable Spring Security filters for MockMvc
public class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @MockBean
    private PlayersService playerService; // Mock the direct dependency

    @MockBean
    private ComprehensivePredictionInputService comprehensivePredictionInputService; // Mock the other direct dependency

    // Mock security-related beans if they are indirectly pulled or for consistency with other tests
    @MockBean
    private UserService userService;
    @MockBean
    private JwtToken jwtToken;

    @Test
    void getAllPlayers_shouldReturnOkAndListOfPlayers_whenPlayersExist() throws Exception {
        Players player1 = new Players("Player One", "10(1)", 5, 2, 7.5);
        // If Players has an ID and it's part of the JSON response, set it.
        // player1.setId(1L); 
        List<Players> playersList = List.of(player1);

        when(playerService.findAllPlayers()).thenReturn(playersList);

        mockMvc.perform(get(ApiPaths.API_BASE + "/playersEntity")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Player One")));
    }

    @Test
    void getAllPlayers_shouldReturnOkAndEmptyList_whenNoPlayersExist() throws Exception {
        when(playerService.findAllPlayers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get(ApiPaths.API_BASE + "/playersEntity")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllPlayers_shouldReturnInternalServerError_whenServiceThrowsException() throws Exception {
        when(playerService.findAllPlayers()).thenThrow(new RuntimeException("Service failure"));

        mockMvc.perform(get(ApiPaths.API_BASE + "/playersEntity")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getPlayersByName_shouldReturnOkAndListOfPlayers_whenPlayersFound() throws Exception {
        String playerName = "Messi";
        Players foundPlayer = new Players(playerName, "30", 25, 15, 9.0);
        List<Players> playersList = List.of(foundPlayer);

        when(playerService.findPlayersByName(playerName)).thenReturn(playersList);

        mockMvc.perform(get(ApiPaths.API_BASE + "/playersEntity/by-name/{playerName}", playerName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is(playerName)));
    }

    @Test
    void getPlayersByName_shouldReturnNotFound_whenNoPlayersFound() throws Exception {
        String playerName = "UnknownPlayer";
        when(playerService.findPlayersByName(playerName)).thenReturn(Collections.emptyList());

        mockMvc.perform(get(ApiPaths.API_BASE + "/playersEntity/by-name/{playerName}", playerName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void getPlayersByName_shouldReturnInternalServerError_whenServiceThrowsException() throws Exception {
        String playerName = "ErrorPlayer";
        when(playerService.findPlayersByName(playerName)).thenThrow(new RuntimeException("Service failure"));

        mockMvc.perform(get(ApiPaths.API_BASE + "/playersEntity/by-name/{playerName}", playerName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getPlayerSearchHistory_shouldReturnOkAndHistoryList() throws Exception {
        LocalDate date = LocalDate.of(2023, 10, 27);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);

        PredictionLog logEntry = new PredictionLog("Messi", "Found 1 player: [Lionel Messi]", "PLAYER_SEARCH");
        // If PredictionLog has an ID and it's part of the JSON response, set it.
        // logEntry.setId(1L);
        // logEntry.setCreatedAt(LocalDateTime.now()); // setCreatedAt is usually handled by constructor

        List<PredictionLog> historyList = List.of(logEntry);

        when(comprehensivePredictionInputService.getPlayerSearchHistory(startOfDay, endOfDay)).thenReturn(historyList);

        mockMvc.perform(get(ApiPaths.API_BASE + "/playersEntity/search-history")
                .param("date", date.format(DateTimeFormatter.ISO_DATE))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requestData", is("Messi")));
    }

    @Test
    void getPlayerSearchHistory_shouldReturnOkAndEmptyList_whenNoHistoryExists() throws Exception {
        LocalDate date = LocalDate.of(2023, 10, 27);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);

        when(comprehensivePredictionInputService.getPlayerSearchHistory(startOfDay, endOfDay)).thenReturn(Collections.emptyList());

        mockMvc.perform(get(ApiPaths.API_BASE + "/playersEntity/search-history")
                .param("date", date.format(DateTimeFormatter.ISO_DATE))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getPlayerSearchHistory_shouldReturnInternalServerError_whenServiceThrowsException() throws Exception {
        LocalDate date = LocalDate.of(2023, 10, 27);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);

        when(comprehensivePredictionInputService.getPlayerSearchHistory(startOfDay, endOfDay))
            .thenThrow(new RuntimeException("Service failure"));

        mockMvc.perform(get(ApiPaths.API_BASE + "/playersEntity/search-history")
                .param("date", date.format(DateTimeFormatter.ISO_DATE))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}
