package com.dappstp.dappstp.webservices;
import com.dappstp.dappstp.webservices.CLFinalStatsController; // Importar el controlador
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import com.dappstp.dappstp.security.JwtToken; // Importa tu clase JwtToken
import com.dappstp.dappstp.service.scraping.clfinal.CLFinalTeamStatsSummaryScraperService;
import com.dappstp.dappstp.service.scraping.clfinal.dto.TeamStatsSummaryDto;
import com.dappstp.dappstp.config.ApiPaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import com.dappstp.dappstp.service.UserService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = CLFinalStatsController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
public class CLFinalStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CLFinalTeamStatsSummaryScraperService clFinalScraperService;

    @MockBean
    private UserService userService;

    @MockBean // Mockea JwtToken para resolver la dependencia en JwtRequestFilter
    private JwtToken jwtToken;

    @Autowired
    private ObjectMapper objectMapper;

    private TeamStatsSummaryDto mockTeamStatsSummaryDto;
    private final String baseScrapeUrl = ApiPaths.API_BASE + "/stats/cl-final/scrape";

    @BeforeEach
    void setUp() {
        mockTeamStatsSummaryDto = new TeamStatsSummaryDto();
        // Asegúrate de que el DTO tenga el campo esperado para la aserción JSONPath
        mockTeamStatsSummaryDto.setHomeTeamName("Equipo Local de Prueba"); // Asume que TeamStatsSummaryDto tiene este setter
    }

    @Test
    void scrapeCLFinalStats_shouldReturnOkAndStats_whenServiceSucceeds() throws Exception {
        when(clFinalScraperService.scrapeTeamStatsSummary(anyString()))
            .thenReturn(mockTeamStatsSummaryDto);

        mockMvc.perform(get(baseScrapeUrl))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.homeTeamName").exists());
    }
}
