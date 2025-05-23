package com.dappstp.dappstp.service.scraping.clfinal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional; // Aunque no hay DB, por consistencia

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("e2e")
class SimpleScorePredictionScraperServiceE2ETest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleScorePredictionScraperServiceE2ETest.class);

    @Autowired
    private SimpleScorePredictionScraperService simpleScorePredictionScraperService;

    // La URL está hardcodeada en el servicio:
    // "https://es.whoscored.com/matches/1899310/Preview/europa-champions-league-2024-2025-paris-saint-germain-inter"
    private final String MATCH_URL = "https://es.whoscored.com/matches/1899310/Preview/europa-champions-league-2024-2025-paris-saint-germain-inter";

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_E2E_WHOSCORED_SCRAPER_TESTS", matches = "true")
    @Transactional // Mantenido por consistencia, aunque este servicio no interactúa con la BD.
    void scrapeScorePrediction_shouldReturnPredictedScoreString() {
        logger.info("Iniciando test E2E para SimpleScorePredictionScraperService.scrapeScorePrediction()");

        // Act
        Throwable thrown = catchThrowable(() -> {
            String prediction = simpleScorePredictionScraperService.scrapeScorePrediction(MATCH_URL);

            // Assert
            assertThat(prediction).isNotNull().isNotBlank();
            // Ejemplo de aserción más específica si se conoce el formato exacto y los equipos
            // assertThat(prediction).startsWith("Predicción: Paris Saint Germain"); // El nombre del equipo puede variar
            assertThat(prediction).matches("Predicción: .* \\d+ - \\d+ .*");
            logger.info("Predicción scrapeada exitosamente: {}", prediction);
        });

        if (thrown != null) {
            logger.error("El test E2E falló debido a una excepción durante el scraping: {}", thrown.getMessage(), thrown);
            // Si el scraping falla (ej. cambio en la web), el test fallará, lo cual es esperado para un E2E.
            // Podrías querer que el test falle explícitamente aquí o dejar que la excepción lo haga.
            // assertThat(thrown).doesNotThrowAnyException(); // Esto fallaría si thrown no es null
            throw new AssertionError("El scraping falló con una excepción", thrown);
        }
    }
}