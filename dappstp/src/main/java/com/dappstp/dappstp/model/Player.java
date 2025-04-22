package com.dappstp.dappstp.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representa un jugador de fútbol con sus estadísticas básicas y equipo.")
public class Player {

    @Schema(description = "Nombre del jugador.", example = "Lionel Messi")
    private String name;

    @Schema(description = "Equipo actual del jugador.", example = "Inter Miami CF") 
    private String teamName; 

    @Schema(description = "Número total de partidos jugados por el jugador.", example = "30")
    private int matchesPlayed;

    @Schema(description = "Número total de goles marcados por el jugador.", example = "25")
    private int goals;

    @Schema(description = "Número total de asistencias dadas por el jugador.", example = "18")
    private int assists;

    @Schema(description = "Valoración media del rendimiento del jugador.", example = "8.9")
    private double rating;

    public Player(String name, String teamName, int matchesPlayed, int goals, int assists, double rating) {
        this.name = name;
        this.teamName = teamName; 
        this.matchesPlayed = matchesPlayed;
        this.goals = goals;
        this.assists = assists;
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public String getTeamName() { 
        return teamName;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public int getGoals() {
        return goals;
    }

    public int getAssists() {
        return assists;
    }

    public double getRating() {
        return rating;
    }

    public void setName(String name) {
        this.name = name;
    }

     public void setTeamName(String team) { 
        this.teamName = team;
    }

    public void setMatchesPlayed(int matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public void setGoals(int goals) {
        this.goals = goals;
    }

    public void setAssists(int assists) {
        this.assists = assists;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}

