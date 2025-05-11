package com.dappstp.dappstp.model;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

@Entity
@Table(name = "upcoming_match")
@Schema(description = "Representa un partido próximo con información básica.")
public class UpcomingMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "ID interno del equipo al que pertenece este partido.", example = "304")
    private Long teamId;

    @Schema(description = "Nombre del equipo local.", example = "FC Barcelona")
    private String homeTeam;

    @Schema(description = "Nombre del equipo visitante.", example = "Real Madrid")
    private String awayTeam;

    @Schema(description = "Fecha y hora de inicio del partido.", example = "2025-05-10T21:00:00")
    private LocalDateTime kickoff;

    @Schema(description = "Competición o liga a la que pertenece el partido.", example = "La Liga")
    private String competition;

    @Schema(description = "Estadio o ciudad donde se juega el partido.", example = "Camp Nou")
    private String venue;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Estado del partido en el calendario.")
    private MatchStatus status;

    // Constructores
    public UpcomingMatch() {}

    public UpcomingMatch(Long id, Long teamId, String homeTeam, String awayTeam,
                         LocalDateTime kickoff, String competition, String venue,
                         MatchStatus status) {
        this.id = id;
        this.teamId = teamId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.kickoff = kickoff;
        this.competition = competition;
        this.venue = venue;
        this.status = status;
    }

    // Getters y setters
    public Long getId() { 
        return id; 
    }

    public void setId(Long id) { 
        this.id = id; 
    }

    public Long getTeamId() { 
        return teamId; 
    }

    public void setTeamId(Long teamId) { 
        this.teamId = teamId; 
    }

    public String getHomeTeam() { 
        return homeTeam; 
    }

    public void setHomeTeam(String homeTeam) { 
        this.homeTeam = homeTeam; 
    }

    public String getAwayTeam() { 
        return awayTeam; 
    }

    public void setAwayTeam(String awayTeam) { 
        this.awayTeam = awayTeam; 
    }

    public LocalDateTime getKickoff() { 
        return kickoff; 
    }

    public void setKickoff(LocalDateTime kickoff) { 
        this.kickoff = kickoff; 
    }

    public String getCompetition() { 
        return competition; 
    }

    public void setCompetition(String competition) { 
        this.competition = competition; 
    }

    public String getVenue() { 
        return venue; 
    }

    public void setVenue(String venue) { 
        this.venue = venue; 
    }

    public MatchStatus getStatus() { 
        return status; 
    }
    public void setStatus(MatchStatus status) { 
        this.status = status; 
    }
}
