package com.dappstp.dappstp.repository;

import com.dappstp.dappstp.model.MatchStatus;
import com.dappstp.dappstp.model.Team;
import com.dappstp.dappstp.model.UpcomingMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("e2e")
@DataJpaTest
public class UpcomingMatchRepositoryTest {

    @Autowired
    private UpcomingMatchRepository upcomingMatchRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Team team1;
    private Team team2;

    // team1 and team2 are persisted to ensure their IDs are generated and available.
    @BeforeEach
    void setUp() {
        team1 = entityManager.persistFlushFind(new Team(null, "Team One", "url1"));
        team2 = entityManager.persistFlushFind(new Team(null, "Team Two", "url2"));
    }

    private UpcomingMatch createMatch(Long teamId, String home, String away, MatchStatus status, LocalDateTime kickoff) {
        UpcomingMatch match = new UpcomingMatch();
        match.setTeamId(teamId); // Set the teamId directly
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setStatus(status);
        match.setKickoff(kickoff);
        // Set other necessary fields if your UpcomingMatch entity has them (e.g., competition, venue)
        // For example:
        // match.setCompetition("Premier League");
        // match.setVenue("Stadium Name");
        return match;
    }

    @Test
    public void whenSaveMatch_thenMatchIsPersisted() {
        UpcomingMatch newMatch = createMatch(team1.getId(), "Home A", "Away B", MatchStatus.PENDING, LocalDateTime.now().plusDays(1));

        UpcomingMatch savedMatch = upcomingMatchRepository.save(newMatch);

        assertThat(savedMatch).isNotNull();
        assertThat(savedMatch.getId()).isNotNull();
        assertThat(savedMatch.getHomeTeam()).isEqualTo("Home A");
        assertThat(savedMatch.getTeamId()).isEqualTo(team1.getId());
    }

    @Test
    public void whenFindByTeamIdAndStatus_thenReturnMatchingMatches() {
        UpcomingMatch match1_team1_pending = createMatch(team1.getId(), "H1", "A1", MatchStatus.PENDING, LocalDateTime.now().plusDays(2));
        UpcomingMatch match2_team1_played = createMatch(team1.getId(), "H2", "A2", MatchStatus.PLAYED, LocalDateTime.now().minusDays(1));
        UpcomingMatch match3_team2_pending = createMatch(team2.getId(), "H3", "A3", MatchStatus.PENDING, LocalDateTime.now().plusDays(3));

        entityManager.persist(match1_team1_pending);
        entityManager.persist(match2_team1_played);
        entityManager.persist(match3_team2_pending);
        entityManager.flush();

        List<UpcomingMatch> team1PendingMatches = upcomingMatchRepository.findByTeamIdAndStatus(team1.getId(), MatchStatus.PENDING);

        assertThat(team1PendingMatches).hasSize(1);
        assertThat(team1PendingMatches.get(0).getHomeTeam()).isEqualTo("H1");
        assertThat(team1PendingMatches.get(0).getTeamId()).isEqualTo(team1.getId());

        List<UpcomingMatch> team1PlayedMatches = upcomingMatchRepository.findByTeamIdAndStatus(team1.getId(), MatchStatus.PLAYED);
        assertThat(team1PlayedMatches).hasSize(1);
        assertThat(team1PlayedMatches.get(0).getHomeTeam()).isEqualTo("H2");
        assertThat(team1PlayedMatches.get(0).getTeamId()).isEqualTo(team1.getId());


        List<UpcomingMatch> team2PendingMatches = upcomingMatchRepository.findByTeamIdAndStatus(team2.getId(), MatchStatus.PENDING);
        assertThat(team2PendingMatches).hasSize(1);
        assertThat(team2PendingMatches.get(0).getHomeTeam()).isEqualTo("H3");
        assertThat(team2PendingMatches.get(0).getTeamId()).isEqualTo(team2.getId());
    }

    @Test
    public void whenDeleteMatch_thenMatchIsRemoved() {
        UpcomingMatch match = createMatch(team1.getId(), "Delete Home", "Delete Away", MatchStatus.PENDING, LocalDateTime.now().plusDays(5));
        UpcomingMatch persistedMatch = entityManager.persistFlushFind(match);
        Long matchId = persistedMatch.getId();

        upcomingMatchRepository.deleteById(matchId);
        entityManager.flush(); // Ensure the delete operation is flushed to the database
        entityManager.clear(); // Detach all entities from the persistence context

        Optional<UpcomingMatch> deletedMatchOpt = upcomingMatchRepository.findById(matchId);
        assertThat(deletedMatchOpt).isNotPresent();
    }
}
