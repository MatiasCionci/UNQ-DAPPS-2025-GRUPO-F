package com.dappstp.dappstp.factory;

import java.time.LocalDateTime;

import com.dappstp.dappstp.model.MatchStatus;
import com.dappstp.dappstp.model.UpcomingMatch;

public class UpcomingMatchFactory {
    
    public static UpcomingMatch createMatchArsenalAndChelsea() {
        UpcomingMatch match = new UpcomingMatch();
        match.setHomeTeam("Arsenal");
        match.setAwayTeam("Chelsea");
        match.setVenue("Emirates Stadium");
        match.setCompetition("Premier League");
        match.setKickoff(LocalDateTime.of(2025, 5, 10, 16, 0));
        match.setStatus(MatchStatus.PENDING);
        match.setTeamId(1L);
        return match;
    }

    public static UpcomingMatch createMatchManchesterAndLiverpool() {
        UpcomingMatch match = new UpcomingMatch();
        match.setHomeTeam("Manchester City");
        match.setAwayTeam("Liverpool");
        match.setVenue("Emirates Stadium");
        match.setCompetition("Premier League");
        match.setKickoff(LocalDateTime.of(2025, 6, 5, 17, 0));
        match.setStatus(MatchStatus.PENDING);
        match.setTeamId(2L);
        return match;
    }
 }