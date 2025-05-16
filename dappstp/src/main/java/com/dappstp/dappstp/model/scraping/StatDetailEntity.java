package com.dappstp.dappstp.model.scraping;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stat_details")
@Data
@NoArgsConstructor
public class StatDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String homeValue;

    @Column(nullable = false)
    private String awayValue;

    // Relación Many-to-One con TeamStatsSummaryEntity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summary_id", nullable = false)
    private TeamStatsSummaryEntity summary;

    // Constructor para facilitar la creación desde el DTO
    public StatDetailEntity(String label, String homeValue, String awayValue) {
        this.label = label; this.homeValue = homeValue; this.awayValue = awayValue;
    }
}