package com.dappstp.dappstp.model;

import java.time.LocalDate;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import jakarta.persistence.Table;

@Data
@Entity
@Table(name = "player_profiles") // opcional
public class PlayerProfileScraping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String fullName;
    private String currentTeam;
    private int shirtNumber;
    private int age;
    private LocalDate birthDate;
    private int height;
    private String nationality;
    private String position;

    // Getters y setters
}
