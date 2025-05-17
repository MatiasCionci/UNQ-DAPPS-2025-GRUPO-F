package com.dappstp.dappstp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor; // Import NoArgsConstructor
@Data
@NoArgsConstructor // Add NoArgsConstructor for JPA and if @Data doesn't generate it due to other constructors
@Entity
@Table(name = "players_finalcl")
public class Players {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String matches; // ej: "26(2)"
    private int goals;
    private int assists;
    private double rating;

    // Constructor for creating new players (without ID)
    public Players(String name, String matches, int goals, int assists, double rating) {
        this.name = name;
        this.matches = matches;
        this.goals = goals;
        this.assists = assists;
        this.rating = rating;
    }
}
