package com.dappstp.dappstp.service.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import com.dappstp.dappstp.model.Team;
import com.dappstp.dappstp.repository.TeamRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "RUN_E2E_PERFORMANCE_TESTS", matches = "true")
public class UpcomingMatchesE2ETest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TeamRepository teamRepo;

    private Long teamId;

    @BeforeEach
    void setUp() {
        // Limpiar tabla
        teamRepo.deleteAll();
        // Insertar equipo de prueba
        Team team = new Team();
        team.setName("Paris Saint-Germain");
        team.setWhoscoredUrl("https://es.whoscored.com/teams/304/fixtures");
        team = teamRepo.save(team);
        teamId = team.getId();
    }

    @Test
    void endToEnd_upcomingMatches_Works() {
        String url = "http://localhost:" + port + "/api/teams/" + teamId + "/upcoming-matches";
        ResponseEntity<String> resp = rest.getForEntity(url, String.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        // Verificar que devuelve un array JSON
        assertThat(resp.getBody()).startsWith("[");
    }
}
