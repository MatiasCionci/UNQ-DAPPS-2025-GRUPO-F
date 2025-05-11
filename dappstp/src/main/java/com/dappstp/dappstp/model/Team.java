package com.dappstp.dappstp.model;

import jakarta.persistence.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "team")
@Schema(description = "Entidad que representa un equipo (incluye URL de WhoScored).")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador único del equipo.", example = "304")
    private Long id;

    @Column(nullable = false, unique = true)
    @Schema(description = "Nombre legible del equipo.", example = "Paris Saint-Germain")
    private String name;

    @Column(nullable = false)
    @Schema(description = "URL de WhoScored para fixtures o estadísticas.",
            example = "https://es.whoscored.com/teams/304/fixtures/francia-paris-saint-germain")
    private String whoscoredUrl;

    // Constructores
    public Team() {}

    public Team(Long id, String name, String whoscoredUrl) {
        this.id = id;
        this.name = name;
        this.whoscoredUrl = whoscoredUrl;
    }

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWhoscoredUrl() {
        return whoscoredUrl;
    }

    public void setWhoscoredUrl(String whoscoredUrl) {
        this.whoscoredUrl = whoscoredUrl;
    }
}
