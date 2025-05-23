package com.dappstp.dappstp.webservices;

import com.dappstp.dappstp.config.ApiPaths;
import com.dappstp.dappstp.model.Prediction;
import com.dappstp.dappstp.model.queryhistory.PredictionLog;
import com.dappstp.dappstp.security.JwtToken;
import com.dappstp.dappstp.service.UserService;
import com.dappstp.dappstp.service.predictionia.PredictionService;
import com.dappstp.dappstp.service.scraping.clfinal.ComprehensivePredictionInputService;
import com.dappstp.dappstp.webservices.dto.PredictionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;

@WebMvcTest(
    controllers = PredictionController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
class PredictionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PredictionService predictionService;

    @MockBean
    private ComprehensivePredictionInputService comprehensivePredictionInputService;

    @MockBean
    private UserService userService; // Mockear dependencias de seguridad si son cargadas

    @MockBean
    private JwtToken jwtToken; // Mockear dependencias de seguridad

    @Autowired
    private ObjectMapper objectMapper;

    private Prediction mockPrediction;

    @BeforeEach
    void setUp() {
        mockPrediction = new Prediction();
        // Configura campos de mockPrediction si es necesario para las aserciones
        // ej. mockPrediction.setPredictedWinner("Team A");
    }

    @Test
    void generatePrediction_shouldReturnOk_whenRequestIsValid() throws Exception {
        PredictionRequest request = new PredictionRequest();
        request.setMatchId("match123");
        request.setScrapedData("Some valid scraped data");
        when(predictionService.analyzeMatch(anyString())).thenReturn(mockPrediction);

        mockMvc.perform(post(ApiPaths.PREDICTIONS_PP)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("success")));
            // .andExpect(jsonPath("$.prediction.predictedWinner", is("Team A"))); // Si configuras el mockPrediction
    }

    @Test
    void generatePrediction_shouldReturnBadRequest_whenScrapedDataIsBlank() throws Exception {
        PredictionRequest request = new PredictionRequest();
        request.setMatchId("match123");
        request.setScrapedData(""); // Blank data

        mockMvc.perform(post(ApiPaths.PREDICTIONS_PP)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("Datos de análisis no pueden estar vacíos")));
    }

    @Test
    void generatePrediction_shouldReturnInternalServerError_whenServiceThrowsException() throws Exception {
        PredictionRequest request = new PredictionRequest();
        // Set fields according to the actual PredictionRequest definition
        request.setMatchId("match123");
        request.setScrapedData("Some data");
        when(predictionService.analyzeMatch(anyString())).thenThrow(new RuntimeException("Analysis failed"));

        mockMvc.perform(post(ApiPaths.PREDICTIONS_PP)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error", is("Error generando predicción: Analysis failed")));

    }

    @Test
    void generateComprehensivePrediction_shouldReturnOk() throws Exception {
        when(comprehensivePredictionInputService.generateAndLogComprehensivePrediction()).thenReturn(mockPrediction);

        mockMvc.perform(get(ApiPaths.PREDICTIONS_PP + "/generate-comprehensive"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("success")));
    }

    @Test
    void generateComprehensivePrediction_shouldReturnInternalServerError_whenServiceThrowsException() throws Exception {
        when(comprehensivePredictionInputService.generateAndLogComprehensivePrediction()).thenThrow(new RuntimeException("Comprehensive failed"));

        mockMvc.perform(get(ApiPaths.PREDICTIONS_PP + "/generate-comprehensive"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error", is("Error generando predicción integral: Comprehensive failed")));
    }

    @Test
    void getPredictionHistory_shouldReturnOkAndHistory() throws Exception {
        LocalDate testDate = LocalDate.of(2023, 10, 27);
        PredictionLog logEntry = new PredictionLog("input", "output", "COMPREHENSIVE");
        List<PredictionLog> mockHistory = Collections.singletonList(logEntry);

        when(comprehensivePredictionInputService.getPredictionHistory(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockHistory);

        mockMvc.perform(get(ApiPaths.PREDICTIONS_PP + "/history")
                .param("date", testDate.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].predictionType", is("COMPREHENSIVE")));
    }

    @Test
    void getPredictionHistory_shouldReturnInternalServerError_whenServiceThrowsException() throws Exception {
        LocalDate testDate = LocalDate.of(2023, 10, 27);
        when(comprehensivePredictionInputService.getPredictionHistory(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("History retrieval failed"));

        mockMvc.perform(get(ApiPaths.PREDICTIONS_PP + "/history")
                .param("date", testDate.toString()))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error", is("Error obteniendo historial: History retrieval failed")));
    }
}