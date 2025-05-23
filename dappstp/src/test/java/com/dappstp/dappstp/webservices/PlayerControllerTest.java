package com.dappstp.dappstp.webservices;

import com.dappstp.dappstp.config.ApiPaths;
import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.security.JwtToken;
import com.dappstp.dappstp.service.PlayersService;
import com.dappstp.dappstp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@WebMvcTest(
    controllers = PlayerController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayersService playersService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtToken jwtToken;

    @Autowired
    private ObjectMapper objectMapper;

    private final String basePlayersUrl = ApiPaths.API_BASE + "/playersEntity";
    private Players player1, player2;

    @BeforeEach
    void setUp() {
        player1 = new Players(); // Asume que Players tiene un constructor por defecto
        player1.setName("Player One"); // Asume setters o usa un constructor con argumentos
        // Configura otros campos de player1 si es necesario para la aserción

        player2 = new Players();
        player2.setName("Player Two");
    }

    @Test
    void getAllPlayers_shouldReturnOkAndListOfPlayers_whenPlayersExist() throws Exception {
        List<Players> mockPlayersList = Arrays.asList(player1, player2);
        when(playersService.findAllPlayers()).thenReturn(mockPlayersList);

        mockMvc.perform(get(basePlayersUrl)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name", is("Player One")))
            .andExpect(jsonPath("$[1].name", is("Player Two")));
    }

    @Test
    void getAllPlayers_shouldReturnOkAndEmptyList_whenNoPlayersExist() throws Exception {
        when(playersService.findAllPlayers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get(basePlayersUrl)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllPlayers_shouldReturnInternalServerError_whenServiceThrowsException() throws Exception {
        when(playersService.findAllPlayers()).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get(basePlayersUrl)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());
    }
}