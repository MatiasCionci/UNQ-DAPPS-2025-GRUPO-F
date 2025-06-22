package com.dappstp.dappstp.dto.metricasAvanzadas;
import lombok.Builder;
import lombok.Data;

/**
 * DTO para representar las estadísticas resumidas de un equipo
 * obtenidas de la fila "Total / Promedio" de la tabla de estadísticas.
 */
@Data
@Builder
public class TeamDataDto {
    private int apps;
    private int goals;
    private double shotsPerGame;
    private int yellowCards;
    private int redCards;
    private double possessionPercentage;
    private double passSuccessPercentage;
    private double aerialsWonPerGame;
    private double rating;
}
