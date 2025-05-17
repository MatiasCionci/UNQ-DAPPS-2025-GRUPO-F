package com.dappstp.dappstp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dappstp.dappstp.model.queryhistory.PredictionLog;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PredictionLogRepository extends JpaRepository<PredictionLog, Long> {

    // Método para buscar logs entre dos fechas
    List<PredictionLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

}