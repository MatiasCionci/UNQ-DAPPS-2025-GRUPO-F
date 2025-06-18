package com.dappstp.dappstp.webservices;

import com.dappstp.dappstp.config.ApiPaths;
import com.dappstp.dappstp.security.JwtToken;
import com.dappstp.dappstp.service.UserService;
import com.dappstp.dappstp.service.getapifootball.FootballApiService;
import com.dappstp.dappstp.dto.MatchesApiResponseDto;
import com.dappstp.dappstp.dto.MatchDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(
    controllers = FootballController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
class FootballControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FootballApiService footballApiService;

    @MockBean // Necesario si JwtRequestFilter está activo y no se excluye completamente
    private UserService userService;

    @MockBean
    private JwtToken jwtToken;

    private MatchesApiResponseDto mockMatchesResponse;
    private final String baseFootballUrl = ApiPaths.API_BASE + "/v1/football";

    @BeforeEach
    void setUp() {
        mockMatchesResponse = new MatchesApiResponseDto();
        MatchDto match = new MatchDto() ; // Asume que MatchDto existe y tiene un campo, p.ej., id
        // match.setId(1L); // Si MatchDto tiene un ID o algún campo para verificar
        mockMatchesResponse.setMatches(Collections.singletonList(match));
    }

    @Test
    void getAllMatches_shouldReturnOkAndMatches_whenServiceSucceedsWithData() throws Exception {
        when(footballApiService.getMatches(anyString())).thenReturn(mockMatchesResponse);

        mockMvc.perform(get(baseFootballUrl + "/matches")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matches").isArray())
            .andExpect(jsonPath("$.matches.length()").value(1));
    }

    @Test
    void getAllMatches_shouldReturnOkAndEmptyMatches_whenServiceReturnsEmpty() throws Exception {
        MatchesApiResponseDto emptyResponse = new MatchesApiResponseDto();
        emptyResponse.setMatches(Collections.emptyList());
        when(footballApiService.getMatches(anyString())).thenReturn(emptyResponse);

        mockMvc.perform(get(baseFootballUrl + "/matches")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matches").isArray())
            .andExpect(jsonPath("$.matches.length()").value(0));
    }

    @Test
    void getAllMatches_shouldReturnInternalServerError_whenServiceReturnsNull() throws Exception {
        when(footballApiService.getMatches(anyString())).thenReturn(null);

        mockMvc.perform(get(baseFootballUrl + "/matches")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());
            // Puedes añadir .andExpect(jsonPath("$.error").value("Mensaje de error específico")) si tu controlador devuelve un ErrorResponse DTO
    }
}