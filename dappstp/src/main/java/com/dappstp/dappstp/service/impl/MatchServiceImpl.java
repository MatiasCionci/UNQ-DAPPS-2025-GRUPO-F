package com.dappstp.dappstp.service.impl;

import java.util.List;

import com.dappstp.dappstp.model.Team;
import com.dappstp.dappstp.model.UpcomingMatch;
import com.dappstp.dappstp.repository.TeamRepository;
import com.dappstp.dappstp.service.MatchService;
import com.dappstp.dappstp.service.scraping.ScraperMatchesService;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MatchServiceImpl implements MatchService {
    
    private final ScraperMatchesService scraper;
    private final TeamRepository teamRepo;

    public MatchServiceImpl(ScraperMatchesService scraper,
                            TeamRepository teamRepo) {
        this.scraper  = scraper;
        this.teamRepo = teamRepo;
    }

    @Override
    public List<UpcomingMatch> getAndPersistUpcomingMatches(Long teamId) {
        Team team = teamRepo.findById(teamId)
                    .orElseThrow(() -> new EntityNotFoundException("Team no existe"));
        return scraper.scrapeAndSync(teamId, team.getWhoscoredUrl());            
    }
}
