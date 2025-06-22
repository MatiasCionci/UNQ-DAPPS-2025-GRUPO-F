package com.dappstp.dappstp.dto.metricasAvanzadas;
import lombok.Data;

@Data
public class AdvancedPlayerStatsDto {
    private double goalsPerGame;
    private double shotConversionRate;
    private double goalContributionsPerGame; // Si tienes asistencias
    // ... otras métricas que decidas añadir
}
