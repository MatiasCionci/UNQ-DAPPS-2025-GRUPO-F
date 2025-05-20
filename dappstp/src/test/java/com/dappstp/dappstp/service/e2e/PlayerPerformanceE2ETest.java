package com.dappstp.dappstp.service.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.repository.PlayersRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "RUN_E2E_PERFORMANCE_TESTS", matches = "true")
public class PlayerPerformanceE2ETest {

    private static final Logger log = LoggerFactory.getLogger(PlayerPerformanceE2ETest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private PlayersRepository repo;

    private Long playerId;

    @BeforeEach
    void setUp() {
        repo.deleteAll();
        Players player = new Players(
            "Test Player",   // name
            "20(0)",         // matches
            10,              // goals
            5,               // assists
            7.5              // rating
        );
        player = repo.save(player);
        playerId = player.getId();
    }

    @Test
    void performanceEndpoint_endToEnd_Works() {
        String url = "http://localhost:" + port + "/api/players/" + playerId + "/performance";
        log.info("Calling URL: {}", url);

        ResponseEntity<String> resp = rest.getForEntity(url, String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).contains("\"playerId\":\"" + playerId + "\"");
    }
}
