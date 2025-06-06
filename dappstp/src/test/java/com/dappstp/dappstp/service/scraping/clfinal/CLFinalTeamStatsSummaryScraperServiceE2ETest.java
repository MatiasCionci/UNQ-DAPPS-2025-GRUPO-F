package com.dappstp.dappstp.service.scraping.clfinal;

import com.dappstp.dappstp.model.scraping.TeamStatsSummaryEntity;
import com.dappstp.dappstp.repository.TeamStatsSummaryRepository;
import com.dappstp.dappstp.service.scraping.clfinal.dto.TeamStatsSummaryDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("e2e")
class CLFinalTeamStatsSummaryScraperServiceE2ETest {

    private static final Logger logger = LoggerFactory.getLogger(CLFinalTeamStatsSummaryScraperServiceE2ETest.class);

    @Autowired
    private CLFinalTeamStatsSummaryScraperService clFinalTeamStatsSummaryScraperService;

    @Autowired
    private TeamStatsSummaryRepository teamStatsSummaryRepository;

    // La URL está hardcodeada en el servicio:
    // "https://es.whoscored.com/matches/1899310/MatchReport/europa-champions-league-2024-2025-paris-saint-germain-inter"
    private final String MATCH_URL = "https://es.whoscored.com/matches/1899310/MatchReport/europa-champions-league-2024-2025-paris-saint-germain-inter";

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_E2E_WHOSCORED_SCRAPER_TESTS", matches = "true")
    @Transactional // Asegura que los cambios en la BD se reviertan después del test
    void scrapeTeamStatsSummary_shouldScrapeAndPersistSummary() {
        logger.info("Iniciando test E2E para CLFinalTeamStatsSummaryScraperService.scrapeTeamStatsSummary()");

        // Act
        Throwable thrown = catchThrowable(() -> {
            TeamStatsSummaryDto scrapedDto = clFinalTeamStatsSummaryScraperService.scrapeTeamStatsSummary(MATCH_URL);

            // Assert DTO
            assertThat(scrapedDto).isNotNull();
            assertThat(scrapedDto.getHomeTeamEmblemUrl()).isNotBlank();
            assertThat(scrapedDto.getAwayTeamEmblemUrl()).isNotBlank();
            assertThat(scrapedDto.getHomeMatchesPlayed()).isNotNull(); // Puede ser "0"
            assertThat(scrapedDto.getAwayMatchesPlayed()).isNotNull(); // Puede ser "0"

            if (scrapedDto.getStats() == null || scrapedDto.getStats().isEmpty()) {
                logger.warn("No se encontraron estadísticas detalladas (stats). Esto puede ser normal si la página no tiene datos o ha cambiado.");
            } else {
                logger.info("Se encontraron {} detalles de estadísticas. Verificando...", scrapedDto.getStats().size());
                assertThat(scrapedDto.getStats()).allSatisfy(statDetailDto -> {
                    assertThat(statDetailDto.getLabel()).isNotBlank();
                    assertThat(statDetailDto.getHomeValue()).isNotNull();
                    assertThat(statDetailDto.getAwayValue()).isNotNull();
                });
            }

            // Assert Persistencia
            // El servicio guarda la entidad, podemos buscarla por el ID (asumiendo que es la última insertada en esta tx)
            Optional<TeamStatsSummaryEntity> savedEntityOpt = teamStatsSummaryRepository.findTopByOrderByIdDesc();
            assertThat(savedEntityOpt).isPresent();
            TeamStatsSummaryEntity savedEntity = savedEntityOpt.get();

            assertThat(savedEntity.getHomeTeamEmblemUrl()).isEqualTo(scrapedDto.getHomeTeamEmblemUrl());
            assertThat(savedEntity.getAwayTeamEmblemUrl()).isEqualTo(scrapedDto.getAwayTeamEmblemUrl());
            assertThat(savedEntity.getHomeMatchesPlayed()).isEqualTo(scrapedDto.getHomeMatchesPlayed());
            assertThat(savedEntity.getAwayMatchesPlayed()).isEqualTo(scrapedDto.getAwayMatchesPlayed());
            assertThat(savedEntity.getStats()).hasSameSizeAs(scrapedDto.getStats());
            // Podríamos hacer una comparación más profunda de las StatDetailEntity con StatDetailDto si es necesario.

            logger.info("Resumen de estadísticas scrapeado y guardado correctamente. ID Entidad: {}", savedEntity.getId());
        });

        if (thrown != null) {
            logger.error("El test E2E falló debido a una excepción durante el scraping o aserción: {}", thrown.getMessage(), thrown);
            throw new AssertionError("El scraping de resumen de estadísticas falló con una excepción", thrown);
        }
    }
}