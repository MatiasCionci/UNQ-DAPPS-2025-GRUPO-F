package com.dappstp.dappstp.service.scraping.championsLeagueFinal;


import com.dappstp.dappstp.model.Prediction;
import com.dappstp.dappstp.model.queryhistory.PredictionLog;
import com.dappstp.dappstp.dto.championsLeague.TeamStatsSummaryDto;
import com.dappstp.dappstp.dto.footballData.MatchesApiResponseDto;
import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.service.PlayersService;
import com.dappstp.dappstp.service.getapifootball.FootballApiService;
import com.dappstp.dappstp.repository.PredictionLogRepository;
import com.dappstp.dappstp.service.predictionia.PredictionService; // Necesario para llamar a analyzeMatch

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime; // Para el nuevo método de historial
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper; // Para convertir Prediction a JSON

@Service
public class ComprehensivePredictionInputService {

    private static final Logger logger = LoggerFactory.getLogger(ComprehensivePredictionInputService.class);

    private final FootballApiService footballApiService;
    private final PlayersService playersService;
    private final CLFinalTeamStatsSummaryScraperService clFinalScraperService;
    private final PredictionService predictionService; // Añadido para llamar a analyzeMatch
    private final PredictionLogRepository predictionLogRepository; // Para guardar logs
    private final ObjectMapper objectMapper; // Para serializar Prediction a JSON

  
    public ComprehensivePredictionInputService(FootballApiService footballApiService,
                                             PlayersService playersService,
                                             CLFinalTeamStatsSummaryScraperService clFinalScraperService,
                                             PredictionService predictionService,
                                             PredictionLogRepository predictionLogRepository,
                                             ObjectMapper objectMapper) {
        this.footballApiService = footballApiService;
        this.playersService = playersService;
        this.clFinalScraperService = clFinalScraperService;
        this.predictionService = predictionService;
        this.predictionLogRepository = predictionLogRepository;
        this.objectMapper = objectMapper;
    }

    public String aggregateDataForPrediction() {
        logger.info("Agregando datos para la predicción integral en paralelo...");
        StringBuilder comprehensiveData = new StringBuilder();
 // Se crea un pool de 4 hilos. Considera inyectar un ExecutorService gestionado por Spring
        // si esta operación se realiza con mucha frecuencia para mejor gestión de recursos.
        ExecutorService executor = Executors.newFixedThreadPool(4);

        Future<MatchesApiResponseDto> interMatchesFuture = executor.submit(() -> {
            logger.debug("Obteniendo partidos del Inter (ID 108) en hilo separado...");
            return footballApiService.getMatches("108");
        });

        Future<MatchesApiResponseDto> psgMatchesFuture = executor.submit(() -> {
            logger.debug("Obteniendo partidos del PSG (ID 524) en hilo separado...");
            return footballApiService.getMatches("524");
        });

        Future<List<Players>> playersListFuture = executor.submit(() -> {
            logger.debug("Obteniendo todos los jugadores en hilo separado...");
            return playersService.findAllPlayers();
        });

        Future<TeamStatsSummaryDto> clFinalStatsFuture = executor.submit(() -> {
            logger.debug("Obteniendo estadísticas de la final de la CL desde la BD en hilo separado...");
            return clFinalScraperService.retrieveLatestTeamStatsSummaryFromDB();
        });

        try {
            // 1. Partidos del Inter
            MatchesApiResponseDto interMatches = null;
            try {
                interMatches = interMatchesFuture.get(); // Espera a que la tarea termine
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Obtención de partidos del Inter interrumpida", e);
            } catch (ExecutionException e) {
                logger.error("Error al obtener partidos del Inter", e.getCause());
            }
            comprehensiveData.append("INTER_MATCHES_START\n");
            comprehensiveData.append(Optional.ofNullable(interMatches).map(Object::toString).orElse("DATOS_INTER_NO_DISPONIBLES"));
            comprehensiveData.append("\nINTER_MATCHES_END\n\n");

            // 2. Partidos del PSG
            MatchesApiResponseDto psgMatches = null;
            try {
                psgMatches = psgMatchesFuture.get(); // Espera a que la tarea termine
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Obtención de partidos del PSG interrumpida", e);
            } catch (ExecutionException e) {
                logger.error("Error al obtener partidos del PSG", e.getCause());
            }
            comprehensiveData.append("PSG_MATCHES_START\n");
            comprehensiveData.append(Optional.ofNullable(psgMatches).map(Object::toString).orElse("DATOS_PSG_NO_DISPONIBLES"));
            comprehensiveData.append("\nPSG_MATCHES_END\n\n");

            // 3. Todos los jugadores
            List<Players> playersList = null;
            try {
                playersList = playersListFuture.get(); // Espera a que la tarea termine
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Obtención de lista de jugadores interrumpida", e);
            } catch (ExecutionException e) {
                logger.error("Error al obtener lista de jugadores", e.getCause());
            }
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
            TeamStatsSummaryDto clFinalStats = null;
            try {
                clFinalStats = clFinalStatsFuture.get(); // Espera a que la tarea termine
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Obtención de estadísticas de la CL interrumpida", e);
            } catch (ExecutionException e) {
                logger.error("Error al obtener estadísticas de la CL", e.getCause());
            }
            comprehensiveData.append("CL_FINAL_STATS_START\n");
            comprehensiveData.append(Optional.ofNullable(clFinalStats).map(Object::toString).orElse("STATS_CL_FINAL_NO_DISPONIBLES"));
            comprehensiveData.append("\nCL_FINAL_STATS_END\n");

        } finally {
            executor.shutdown(); // Inicia el apagado ordenado del executor
            try {
                // Espera un tiempo para que las tareas terminen
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Cancela las tareas que no hayan terminado
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 1. Partidos del Inter
   

        logger.info("Datos agregados exitosamente.");
        return comprehensiveData.toString();
    }

    // Método para generar la predicción y guardar el log
    public Prediction generateAndLogComprehensivePrediction() {
        String inputData = aggregateDataForPrediction();
        Prediction prediction = predictionService.analyzeMatch(inputData);

        try {
            String predictionJson = objectMapper.writeValueAsString(prediction);
            PredictionLog logEntry = new PredictionLog(inputData, predictionJson, "COMPREHENSIVE");
            predictionLogRepository.save(logEntry);
            logger.info("Predicción integral registrada con ID de log: {}", logEntry.getId());
        } catch (Exception e) {
            logger.error("Error al registrar la predicción integral: {}", e.getMessage(), e);
            // Decidir si relanzar la excepción o solo loguear
        }
        return prediction;
    }

    // Nuevo método para obtener el historial de predicciones
    public List<PredictionLog> getPredictionHistory(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Consultando historial de predicciones desde {} hasta {}", startDate, endDate);
        // Asegurarse de que endDate sea el final del día si solo se pasa una fecha
        if (startDate.toLocalDate().equals(endDate.toLocalDate())) {
            endDate = endDate.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        }
        return predictionLogRepository.findByCreatedAtBetween(startDate, endDate);
    }
    public List<PredictionLog> getPlayerSearchHistory(LocalDateTime startDate, LocalDateTime endDate) {
    logger.info("Consultando historial de búsqueda de jugadores desde {} hasta {}", startDate, endDate);
    // Asegurarse de que endDate sea el final del día si solo se pasa una fecha
    if (startDate.toLocalDate().equals(endDate.toLocalDate())) {
        endDate = endDate.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
    }
    return predictionLogRepository.findByPredictionTypeAndCreatedAtBetween("PLAYER_SEARCH", startDate, endDate);
    }
}