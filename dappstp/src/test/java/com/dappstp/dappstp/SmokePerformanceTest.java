package com.dappstp.dappstp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.dappstp.dappstp.webservices.PlayerPerformanceController; 
import com.dappstp.dappstp.service.PlayerPerformanceService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class SmokePerformanceTest {
    private MockMvc mvc;

    @Mock
    private PlayerPerformanceService performanceService;

    @InjectMocks
    private PlayerPerformanceController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void endpointPerformanceExiste() throws Exception {
        mvc.perform(get("/api/players/1/performance"))
           .andExpect(status().is2xxSuccessful()); // Ruta existe y devuelve 200 OK con mock
    }
}
