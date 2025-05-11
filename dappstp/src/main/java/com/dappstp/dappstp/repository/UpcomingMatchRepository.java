package com.dappstp.dappstp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dappstp.dappstp.model.MatchStatus;
import com.dappstp.dappstp.model.UpcomingMatch;

@Repository
public interface UpcomingMatchRepository extends JpaRepository<UpcomingMatch, Long> {
    List<UpcomingMatch> findByTeamIdAndStatus(Long teamId, MatchStatus status);
}