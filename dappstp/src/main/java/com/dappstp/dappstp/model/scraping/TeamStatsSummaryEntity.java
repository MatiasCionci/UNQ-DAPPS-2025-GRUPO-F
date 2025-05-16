package com.dappstp.dappstp.model.scraping;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "team_stats_summaries")
@Data
@NoArgsConstructor
public class TeamStatsSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Puedes añadir una columna para la URL del partido si quieres vincular el resumen
    // @Column(unique = true) // Si solo quieres un resumen por URL
    // private String matchUrl;

    private String homeTeamName;
    private String awayTeamName;
    private String homeTeamEmblemUrl;
    private String homeMatchesPlayed;
    private String awayTeamEmblemUrl;
    private String awayMatchesPlayed;

    // Relación One-to-Many con StatDetailEntity
    // CascadeType.ALL: Si guardas/borras el resumen, también se guardan/borran las estadísticas asociadas.
    // orphanRemoval = true: Si eliminas una estadística de la lista, se borra de la base de datos.
    @OneToMany(mappedBy = "summary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StatDetailEntity> stats = new ArrayList<>();

    // Método helper para añadir estadísticas y mantener la relación bidireccional
    public void addStatDetail(StatDetailEntity statDetail) {
        stats.add(statDetail);
        statDetail.setSummary(this);
    }

    // Método helper para remover estadísticas
    public void removeStatDetail(StatDetailEntity statDetail) {
        stats.remove(statDetail);
        statDetail.setSummary(null);
    }
}