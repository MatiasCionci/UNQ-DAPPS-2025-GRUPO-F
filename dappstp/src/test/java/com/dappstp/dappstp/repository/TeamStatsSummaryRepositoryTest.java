package com.dappstp.dappstp.repository;

import com.dappstp.dappstp.model.scraping.TeamStatsSummaryEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
@ActiveProfiles("e2e")
@DataJpaTest
public class TeamStatsSummaryRepositoryTest {

    @Autowired
    private TeamStatsSummaryRepository teamStatsSummaryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private TeamStatsSummaryEntity createSummary(String homeTeamName) {
        TeamStatsSummaryEntity summary = new TeamStatsSummaryEntity();
        summary.setHomeTeamName(homeTeamName);
        summary.setAwayTeamName("Away Test");
        // Set other necessary fields if they are not nullable
        return summary;
    }

    @Test
    public void whenSaveSummary_thenSummaryIsPersisted() {
        TeamStatsSummaryEntity newSummary = createSummary("Home Team Alpha");

        TeamStatsSummaryEntity savedSummary = teamStatsSummaryRepository.save(newSummary);

        assertThat(savedSummary).isNotNull();
        assertThat(savedSummary.getId()).isNotNull();
        assertThat(savedSummary.getHomeTeamName()).isEqualTo("Home Team Alpha");
    }

    @Test
    public void whenFindTopByOrderByIdDesc_thenReturnLatestSummary() {
        TeamStatsSummaryEntity summary1 = createSummary("Summary 1");
        TeamStatsSummaryEntity summary2 = createSummary("Summary 2 - Latest"); // Will have higher ID

        entityManager.persist(summary1);
        entityManager.persist(summary2);
        entityManager.flush();

        Optional<TeamStatsSummaryEntity> foundOpt = teamStatsSummaryRepository.findTopByOrderByIdDesc();

        assertThat(foundOpt).isPresent();
        assertThat(foundOpt.get().getHomeTeamName()).isEqualTo("Summary 2 - Latest");
        assertThat(foundOpt.get().getId()).isEqualTo(summary2.getId());
    }

    @Test
    public void whenFindTopByOrderByIdDesc_withNoData_thenReturnEmpty() {
        Optional<TeamStatsSummaryEntity> foundOpt = teamStatsSummaryRepository.findTopByOrderByIdDesc();
        assertThat(foundOpt).isNotPresent();
    }

    @Test
    public void whenDeleteSummary_thenSummaryIsRemoved() {
        TeamStatsSummaryEntity summary = createSummary("Summary to Delete");
        TeamStatsSummaryEntity persistedSummary = entityManager.persistFlushFind(summary);
        Long summaryId = persistedSummary.getId();

        teamStatsSummaryRepository.deleteById(summaryId);
        entityManager.flush();
        entityManager.clear();

        Optional<TeamStatsSummaryEntity> deletedSummaryOpt = teamStatsSummaryRepository.findById(summaryId);
        assertThat(deletedSummaryOpt).isNotPresent();
    }
}