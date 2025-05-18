package com.dappstp.dappstp.service.getapifootball;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // Carga el contexto completo de la aplicación Spring Boot
@ActiveProfiles("e2e") 
class FootballApiServiceE2ETest {

    private static final Logger logger = LoggerFactory.getLogger(FootballApiServiceE2ETest.class);

    @Autowired
    private FootballApiService footballApiService;

    // Para que este test funcione, necesitas un bean de RestTemplate configurado en tu aplicación.
    // Si no lo tienes, puedes añadir una configuración como esta en una clase @Configuration:
    // @Bean
    // public RestTemplate restTemplate() {
    //     return new RestTemplate();
    // }
    // O asegurarte de que FootballApiService lo reciba correctamente.

    @Test
    // Opcional: Ejecutar este test solo si una variable de entorno específica está presente,
    // para evitar ejecutarlo siempre y consumir cuota de API.
    // Por ejemplo, podrías configurar RUN_E2E_FOOTBALL_API_TESTS=true en tu entorno de CI.
    @EnabledIfEnvironmentVariable(named = "RUN_E2E_FOOTBALL_API_TESTS", matches = "true")
    void getMatches_forRealMadrid_shouldReturnMatches() {
        // Arrange
        String realMadridId = "86"; // ID del Real Madrid según el comentario en tu servicio
        logger.info("Iniciando test E2E para FootballApiService.getMatches con ID: {}", realMadridId);

        // Act
        MatchesApiResponseDto response = footballApiService.getMatches(realMadridId);
        logger.info("Respuesta recibida de la API: {}", response != null ? response.getMatches().size() + " partidos" : "null");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMatches()).isNotNull();

        // Puedes añadir aserciones más específicas si conoces la estructura esperada
        // y si los datos son relativamente estables.
        // Por ejemplo, si esperas que siempre haya partidos:
        if (!response.getMatches().isEmpty()) {
            MatchDto firstMatch = response.getMatches().get(0);
            assertThat(firstMatch.getId()).isPositive();
            assertThat(firstMatch.getHomeTeam()).isNotNull();
            assertThat(firstMatch.getHomeTeam().getName()).isNotBlank();
            assertThat(firstMatch.getAwayTeam()).isNotNull();
            assertThat(firstMatch.getAwayTeam().getName()).isNotBlank();
            logger.info("Primer partido encontrado: {} vs {}", firstMatch.getHomeTeam().getName(), firstMatch.getAwayTeam().getName());
        } else {
            logger.warn("La API no devolvió partidos para el ID: {}. Esto podría ser normal o un problema con la API/datos.", realMadridId);
        }
    }
}
