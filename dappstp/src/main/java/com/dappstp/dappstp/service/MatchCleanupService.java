package com.dappstp.dappstp.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.dappstp.dappstp.model.MatchStatus;
import com.dappstp.dappstp.repository.UpcomingMatchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchCleanupService {
    private final UpcomingMatchRepository upComingMatchRepo;

    /**
     * Cada 7 días (en milisegundos), elimina todos los partidos PENDING cuya fecha de kickoff 
     * sea anterior a (ahora menos 7 días).
     */
    @Scheduled(fixedDelay = 7 * 24 * 60 * 60 * 1000L)
    public void cleanPendingOlderThanAWeek() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        int deleted = upComingMatchRepo.deleteByKickoffBeforeAndStatus(threshold, MatchStatus.PENDING);
        log.info("Scheduler: eliminados {} partidos PENDING anteriores a {}", deleted, threshold);
    }
}
