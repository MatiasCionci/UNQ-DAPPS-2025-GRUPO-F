package com.dappstp.dappstp.dto.championsLeague;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TeamStatsSummaryDto {
    private String homeTeamName; // Podríamos añadir nombres si están disponibles en la página
    private String awayTeamName;
    private String homeTeamEmblemUrl;
    private String homeMatchesPlayed;
    private String awayTeamEmblemUrl;
    private String awayMatchesPlayed;
    private List<StatDetailDto> stats;
}