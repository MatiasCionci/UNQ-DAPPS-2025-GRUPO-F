package com.dappstp.dappstp.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.dappstp.dappstp.model.PlayerPerformance;
import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.repository.PlayersRepository;
import com.dappstp.dappstp.service.WhoScoredScraper;

import jakarta.persistence.EntityNotFoundException;

@Service
@Primary
public class DevWhoScoredScraperService implements WhoScoredScraper {

    private final PlayersRepository playersRepository;

    public DevWhoScoredScraperService(PlayersRepository playersRepository) {
        this.playersRepository = playersRepository;
    }

    @Override
    public PlayerPerformance scrapeRecentStats(String playerId) {
        Players p = playersRepository.findById(Long.valueOf(playerId))
            .orElseThrow(() -> new EntityNotFoundException("Jugador no encontrado: " + playerId));

        Map<String, Double> metrics = new HashMap<>();
        metrics.put("goals", (double) p.getGoals());
        metrics.put("assists", (double) p.getAssists());
        // Añadimos "rating" como métrica adicional
        metrics.put("rating", p.getRating());
        

        return new PlayerPerformance(playerId, metrics);
    }

    @Override
    public List<PlayerPerformance> scrapeHistoricalStats(String playerId) {
        // Sin historial para stub -> trend == recent
        return List.of();
    }
}
