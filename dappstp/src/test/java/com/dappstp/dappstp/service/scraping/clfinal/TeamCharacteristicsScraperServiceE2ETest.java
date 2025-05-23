package com.dappstp.dappstp.service.scraping.clfinal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional; // Aunque no hay DB, por consistencia

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("e2e")
class TeamCharacteristicsScraperServiceE2ETest {

    private static final Logger logger = LoggerFactory.getLogger(TeamCharacteristicsScraperServiceE2ETest.class);

    @Autowired
    private TeamCharacteristicsScraperService teamCharacteristicsScraperService;

    // La URL está hardcodeada en el servicio:
    // "https://es.whoscored.com/matches/1899310/Preview/europa-champions-league-2024-2025-paris-saint-germain-inter"
    private final String MATCH_URL = "https://es.whoscored.com/matches/1899310/Preview/europa-champions-league-2024-2025-paris-saint-germain-inter";

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_E2E_WHOSCORED_SCRAPER_TESTS", matches = "true")
    @Transactional // Mantenido por consistencia.
    void scrapeTeamCharacteristics_shouldReturnListOfCharacteristics() {
        logger.info("Iniciando test E2E para TeamCharacteristicsScraperService.scrapeTeamCharacteristics()");

        // Act
        Throwable thrown = catchThrowable(() -> {
            List<String> characteristics = teamCharacteristicsScraperService.scrapeTeamCharacteristics(MATCH_URL);

            // Assert
            assertThat(characteristics).isNotNull();

            if (characteristics.isEmpty()) {
                logger.warn("No se encontraron características de equipo. Esto puede ser normal si la página no tiene datos o ha cambiado.");
            } else {
                logger.info("Se encontraron {} características de equipo. Verificando que no estén vacías...", characteristics.size());
                assertThat(characteristics).allSatisfy(characteristic -> {
                    assertThat(characteristic).isNotBlank();
                });
                logger.info("Características scrapeadas exitosamente: {}", characteristics);
            }
        });

        if (thrown != null) {
            logger.error("El test E2E falló debido a una excepción durante el scraping: {}", thrown.getMessage(), thrown);
            throw new AssertionError("El scraping de características falló con una excepción", thrown);
        }
    }
}
