package com.dappstp.dappstp.dto.metricasAvanzadas;

import lombok.Data;

/**
 * DTO para representar las estadísticas brutas de un jugador obtenidas de WhoScored.
 * Corresponde a la fila "Total / Promedio".
 */
@Data
public class PlayerRawStatsDto {
    private int apps;
    private int goals;
    private int assists;
    private double shotsPerGame;
    private int discipline;
    private double possessionPercentage;
    private double passSuccessPercentage;
    private double aerialsWon;
    private double rating;
}


