package com.dappstp.dappstp.dto.footballData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchDto {
    private int id;
    private String utcDate;
    private String status;

    @JsonProperty("homeTeam") // Asegura el mapeo correcto si el nombre del campo difiere
    private TeamDto homeTeam;

    @JsonProperty("awayTeam")
    private TeamDto awayTeam;

    // Getters y Setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getUtcDate() {
        return utcDate;
    }
    public void setUtcDate(String utcDate) {
        this.utcDate = utcDate;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public TeamDto getHomeTeam() {
        return homeTeam;
    }
    public void setHomeTeam(TeamDto homeTeam) {
        this.homeTeam = homeTeam;
    }
    public TeamDto getAwayTeam() {
        return awayTeam;
    }
    public void setAwayTeam(TeamDto awayTeam) {
        this.awayTeam = awayTeam;
    }
        @Override
    public String toString() {
        return "MatchDto{" + "id=" + id +
               ", homeTeam=" + (homeTeam != null ? homeTeam.getName() : "N/A") +
               ", awayTeam=" + (awayTeam != null ? awayTeam.getName() : "N/A") +
               ", status='" + status + '\'' + ", utcDate='" + utcDate + '\'' + '}';
    }
}