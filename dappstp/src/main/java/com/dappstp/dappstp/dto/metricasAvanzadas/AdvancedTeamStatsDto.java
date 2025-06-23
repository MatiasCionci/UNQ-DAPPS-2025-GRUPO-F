package com.dappstp.dappstp.dto.metricasAvanzadas;

import lombok.Data;

/**
 * DTO para representar las métricas avanzadas calculadas para un equipo.
 */
@Data
public class AdvancedTeamStatsDto {
    private double goalsPerGame;
    private double shotConversionRate;
    private double cardsPerGame;
    // Puedes añadir más métricas avanzadas aquí en el futuro.
}