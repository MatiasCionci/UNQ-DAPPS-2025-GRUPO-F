package com.dappstp.dappstp.repository;

import com.dappstp.dappstp.model.queryhistory.PredictionLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.ActiveProfiles;
@ActiveProfiles("e2e")
@DataJpaTest
public class PredictionLogRepositoryTest {

    @Autowired
    private PredictionLogRepository predictionLogRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void whenSaveLog_thenLogIsPersisted() {
        PredictionLog newLog = new PredictionLog("requestData1", "resultData1", "type1");
        // newLog.setCreatedAt() is handled by constructor or prePersist if any

        PredictionLog savedLog = predictionLogRepository.save(newLog);

        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getId()).isNotNull();
        assertThat(savedLog.getRequestData()).isEqualTo("requestData1");
        assertThat(savedLog.getCreatedAt()).isNotNull();
    }

    @Test
    public void whenFindById_thenReturnLog() {
        PredictionLog log = new PredictionLog("requestData2", "resultData2", "type2");
        PredictionLog persistedLog = entityManager.persistFlushFind(log);

        Optional<PredictionLog> foundLogOpt = predictionLogRepository.findById(persistedLog.getId());

        assertThat(foundLogOpt).isPresent();
        assertThat(foundLogOpt.get().getPredictionType()).isEqualTo("type2");
    }

    @Test
    public void whenFindByCreatedAtBetween_thenReturnMatchingLogs() {
        LocalDateTime now = LocalDateTime.now();
        PredictionLog log1 = new PredictionLog("req1", "res1", "typeA");
        log1.setCreatedAt(now.minusDays(1)); // Setter for CreatedAt needed for this test

        PredictionLog log2 = new PredictionLog("req2", "res2", "typeB");
        log2.setCreatedAt(now);

        PredictionLog log3 = new PredictionLog("req3", "res3", "typeC");
        log3.setCreatedAt(now.plusDays(1));

        entityManager.persist(log1);
        entityManager.persist(log2);
        entityManager.persist(log3);
        entityManager.flush();

        // Search for logs created 'today' (log2)
        List<PredictionLog> foundLogs = predictionLogRepository.findByCreatedAtBetween(
                now.toLocalDate().atStartOfDay(), // From start of today
                now.toLocalDate().atTime(23, 59, 59) // To end of today
        );

        assertThat(foundLogs).hasSize(1);
        assertThat(foundLogs.get(0).getRequestData()).isEqualTo("req2");

        // Search for logs including yesterday and today
        List<PredictionLog> foundLogsRange = predictionLogRepository.findByCreatedAtBetween(
                now.minusDays(1).toLocalDate().atStartOfDay(),
                now.toLocalDate().atTime(23, 59, 59)
        );
        assertThat(foundLogsRange).hasSize(2).extracting(PredictionLog::getRequestData).containsExactlyInAnyOrder("req1", "req2");
    }

    @Test
    public void whenDeleteLog_thenLogIsRemoved() {
        PredictionLog log = new PredictionLog("reqToDelete", "resToDelete", "typeDel");
        PredictionLog persistedLog = entityManager.persistFlushFind(log);
        Long logId = persistedLog.getId();

        predictionLogRepository.deleteById(logId);
        entityManager.flush();
        entityManager.clear();

        Optional<PredictionLog> deletedLogOpt = predictionLogRepository.findById(logId);
        assertThat(deletedLogOpt).isNotPresent();
    }
}