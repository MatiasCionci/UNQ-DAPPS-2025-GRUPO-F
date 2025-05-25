package com.dappstp.dappstp.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dappstp.dappstp.model.MatchStatus;
import com.dappstp.dappstp.model.UpcomingMatch;

@Repository
public interface UpcomingMatchRepository extends JpaRepository<UpcomingMatch, Long> {
    List<UpcomingMatch> findByTeamIdAndStatus(Long teamId, MatchStatus status);

    /** 
     * Borra todos los partidos PENDING con kickoff anterior a la fecha dada. 
     * Devuelve cuántos registros eliminó.
     */
    int deleteByKickoffBeforeAndStatus(LocalDateTime dateTime, MatchStatus status);
}