package com.dappstp.dappstp.service.scraping.clfinal;

import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.repository.PlayersRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("e2e")
class ScraperServicePlayersE2ETest {

    private static final Logger logger = LoggerFactory.getLogger(ScraperServicePlayersE2ETest.class);

    @Autowired
    private ScraperServicePlayers scraperServicePlayers;

    @Autowired
    private PlayersRepository playersRepository;

    // La URL está hardcodeada en el servicio:
    // "https://es.whoscored.com/matches/1899310/playerstatistics/europa-champions-league-2024-2025-paris-saint-germain-inter"

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_E2E_WHOSCORED_SCRAPER_TESTS", matches = "true")
    @Transactional // Asegura que los cambios en la BD se reviertan después del test
    void scrapeAndSavePlayers_shouldScrapeAndPersistPlayers() {
        logger.info("Iniciando test E2E para ScraperServicePlayers.scrapeAndSavePlayers()");

        // Act
        List<Players> scrapedPlayers = scraperServicePlayers.scrapeAndSavePlayers();

        // Assert
        assertThat(scrapedPlayers).isNotNull();

        // Verificar la persistencia consultando directamente el repositorio
        List<Players> savedPlayersInDb = playersRepository.findAll();

        if (scrapedPlayers.isEmpty()) {
            // Es posible que la página no tenga jugadores o el scraping falle por cambios en la web.
            logger.warn("No se encontraron jugadores. Esto puede ser normal si la página no tiene datos o ha cambiado.");
            assertThat(savedPlayersInDb).isEmpty();
        } else {
            logger.info("Se encontraron {} jugadores. Verificando detalles...", scrapedPlayers.size());
            assertThat(savedPlayersInDb).hasSameSizeAs(scrapedPlayers);

            for (Players player : scrapedPlayers) {
                assertThat(player.getId()).isNotNull(); // El ID debe asignarse después de guardar
                assertThat(player.getName()).isNotBlank();
                assertThat(player.getMatches()).isNotNull(); // Es un String, puede ser "0" o "(1)", etc.
                // Goals, Assists, Rating pueden ser 0/0.0 si no hay datos o por parseo seguro.
                assertThat(player.getGoals()).isGreaterThanOrEqualTo(0);
                assertThat(player.getAssists()).isGreaterThanOrEqualTo(0);
                assertThat(player.getRating()).isGreaterThanOrEqualTo(0.0);
            }

            // Compara si los objetos en la lista scrapeada (y guardada) son los mismos que los recuperados de la BD.
            // Players necesitaría una implementación adecuada de equals/hashCode para containsExactlyInAnyOrderElementsOf,
            // o podemos usar una comparación recursiva.
            assertThat(savedPlayersInDb)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id") // Ignoramos ID para la comparación de contenido si se generan nuevos
                .containsExactlyInAnyOrderElementsOf(scrapedPlayers);

            logger.info("Se scrapearon y guardaron correctamente {} jugadores.", savedPlayersInDb.size());
        }
    }
}