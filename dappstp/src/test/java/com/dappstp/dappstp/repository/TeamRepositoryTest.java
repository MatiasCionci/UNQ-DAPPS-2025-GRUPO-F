package com.dappstp.dappstp.repository;

import com.dappstp.dappstp.model.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.ActiveProfiles;
@ActiveProfiles("e2e")
@DataJpaTest
public class TeamRepositoryTest {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void whenSaveTeam_thenTeamIsPersisted() {
        Team newTeam = new Team(null, "Real Madrid", "http://whoscored.com/realmadrid");

        Team savedTeam = teamRepository.save(newTeam);

        assertThat(savedTeam).isNotNull();
        assertThat(savedTeam.getId()).isNotNull();
        assertThat(savedTeam.getName()).isEqualTo("Real Madrid");
    }

    @Test
    public void whenFindById_thenReturnTeam() {
        Team team = new Team(null, "FC Barcelona", "http://whoscored.com/barcelona");
        Team persistedTeam = entityManager.persistFlushFind(team);

        Optional<Team> foundTeamOpt = teamRepository.findById(persistedTeam.getId());

        assertThat(foundTeamOpt).isPresent();
        assertThat(foundTeamOpt.get().getName()).isEqualTo("FC Barcelona");
    }

    @Test
    public void whenFindAll_thenReturnAllTeams() {
        Team team1 = new Team(null, "Liverpool FC", "http://whoscored.com/liverpool");
        Team team2 = new Team(null, "Manchester City", "http://whoscored.com/mancity");
        entityManager.persist(team1);
        entityManager.persist(team2);
        entityManager.flush();

        List<Team> allTeams = teamRepository.findAll();

        assertThat(allTeams).hasSizeGreaterThanOrEqualTo(2);
        assertThat(allTeams).extracting(Team::getName).contains("Liverpool FC", "Manchester City");
    }

    @Test
    public void whenDeleteTeam_thenTeamIsRemoved() {
        Team team = new Team(null, "Chelsea FC", "http://whoscored.com/chelsea");
        Team persistedTeam = entityManager.persistFlushFind(team);
        Long teamId = persistedTeam.getId();

        teamRepository.deleteById(teamId);
        entityManager.flush();
        entityManager.clear();

        Optional<Team> deletedTeamOpt = teamRepository.findById(teamId);
        assertThat(deletedTeamOpt).isNotPresent();
    }
}