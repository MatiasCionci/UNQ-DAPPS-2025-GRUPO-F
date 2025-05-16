package com.dappstp.dappstp.repository;
import com.dappstp.dappstp.model.scraping.TeamStatsSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamStatsSummaryRepository extends JpaRepository<TeamStatsSummaryEntity, Long> {
    // Puedes añadir métodos de consulta personalizados aquí si los necesitas
}