package com.dappstp.dappstp.service.scraping.clfinal;


import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.service.PlayersService;
import com.dappstp.dappstp.service.getapifootball.FootballApiService;
import com.dappstp.dappstp.service.getapifootball.MatchesApiResponseDto;
import com.dappstp.dappstp.service.scraping.clfinal.CLFinalTeamStatsSummaryScraperService;
import com.dappstp.dappstp.service.scraping.clfinal.dto.TeamStatsSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ComprehensivePredictionInputService {

    private static final Logger logger = LoggerFactory.getLogger(ComprehensivePredictionInputService.class);

    private final FootballApiService footballApiService;
    private final PlayersService playersService;
    private final CLFinalTeamStatsSummaryScraperService clFinalScraperService;

    @Autowired
    public ComprehensivePredictionInputService(FootballApiService footballApiService,
                                             PlayersService playersService,
                                             CLFinalTeamStatsSummaryScraperService clFinalScraperService) {
        this.footballApiService = footballApiService;
        this.playersService = playersService;
        this.clFinalScraperService = clFinalScraperService;
    }

    public String aggregateDataForPrediction() {
        logger.info("Agregando datos para la predicción integral...");
        StringBuilder comprehensiveData = new StringBuilder();

        // 1. Partidos del Inter
        logger.debug("Obteniendo partidos del Inter (ID 108)...");
        MatchesApiResponseDto interMatches = footballApiService.getMatches("108");
        comprehensiveData.append("INTER_MATCHES_START\n");
        comprehensiveData.append(Optional.ofNullable(interMatches).map(Object::toString).orElse("DATOS_INTER_NO_DISPONIBLES"));
        comprehensiveData.append("\nINTER_MATCHES_END\n\n");

        // 2. Partidos del PSG
        logger.debug("Obteniendo partidos del PSG (ID 524)...");
        MatchesApiResponseDto psgMatches = footballApiService.getMatches("524");
        comprehensiveData.append("PSG_MATCHES_START\n");
        comprehensiveData.append(Optional.ofNullable(psgMatches).map(Object::toString).orElse("DATOS_PSG_NO_DISPONIBLES"));
        comprehensiveData.append("\nPSG_MATCHES_END\n\n");

        // 3. Todos los jugadores
        logger.debug("Obteniendo todos los jugadores...");
        List<Players> playersList = playersService.findAllPlayers();
        String playersString = "JUGADORES_NO_DISPONIBLES";
        if (playersList != null && !playersList.isEmpty()) {
            playersString = playersList.stream()
                .map(p -> String.format("Jugador{nombre=%s, partidos=%s, goles=%s, asistencias=%s, rating=%s}",
                                        p.getName(),
                                        String.valueOf(p.getMatches()),
                                        String.valueOf(p.getGoals()),
                                        String.valueOf(p.getAssists()),
                                        String.valueOf(p.getRating())))
                .collect(Collectors.joining("; "));
        }
        comprehensiveData.append("ALL_PLAYERS_START\n");
        comprehensiveData.append(playersString);
        comprehensiveData.append("\nALL_PLAYERS_END\n\n");

        // 4. Estadísticas de la Final de la CL
        logger.debug("Intentando obtener las últimas estadísticas guardadas de la final de la CL desde la BD...");
        TeamStatsSummaryDto clFinalStats = clFinalScraperService.retrieveLatestTeamStatsSummaryFromDB();
        comprehensiveData.append("CL_FINAL_STATS_START\n");
        comprehensiveData.append(Optional.ofNullable(clFinalStats).map(Object::toString).orElse("STATS_CL_FINAL_NO_DISPONIBLES"));
        comprehensiveData.append("\nCL_FINAL_STATS_END\n");

        logger.info("Datos agregados exitosamente.");
        return comprehensiveData.toString();
    }
}