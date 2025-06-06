package com.dappstp.dappstp.webservices;

import java.util.List;

import com.dappstp.dappstp.model.Team;
import com.dappstp.dappstp.model.UpcomingMatch;
import com.dappstp.dappstp.service.MatchService;
import com.dappstp.dappstp.service.scraping.ScraperMatchesService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;

@RestController
@RequestMapping("/api/teams")
public class MatchController {

    private final MatchService matchService;
    private final ScraperMatchesService scraper;

    public MatchController(ScraperMatchesService scraper,MatchService matchService) {
        this.scraper      = scraper;
        this.matchService = matchService;
     
    }

    @PostMapping
public ResponseEntity<Team> createTeam(@RequestBody Team teamPayload) {
    try {
        Team saved = scraper.scrapeAndSave(teamPayload.getWhoscoredUrl());
        URI location = URI.create("/api/teams/" + saved.getId());
        return ResponseEntity.created(location).body(saved);

    } catch (IllegalStateException e) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "No encontré el <h1>: " + e.getMessage(),
            e
        );
    } catch (Exception e) {
        // devolvemos la traza en el mensaje para depurar
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stack = sw.toString();
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Error al scrapear el equipo:\n" + e.getMessage() + "\n" + stack,
            e
        );
    }
}

    @GetMapping("/{teamId}/upcoming-matches")
    public ResponseEntity<List<UpcomingMatch>> getUpcomingMatches(@PathVariable Long teamId) {
        List<UpcomingMatch> matches = matchService.getAndPersistUpcomingMatches(teamId);
        return ResponseEntity.ok(matches);
    }
}
