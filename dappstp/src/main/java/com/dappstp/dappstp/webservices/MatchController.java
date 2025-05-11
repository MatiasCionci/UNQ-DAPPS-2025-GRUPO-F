package com.dappstp.dappstp.webservices;

import java.util.List;
import com.dappstp.dappstp.model.UpcomingMatch;
import com.dappstp.dappstp.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/{teamId}/upcoming-matches")
    public ResponseEntity<List<UpcomingMatch>> getUpcomingMatches(@PathVariable Long teamId) {
        List<UpcomingMatch> matches = matchService.getAndPersistUpcomingMatches(teamId);
        return ResponseEntity.ok(matches);
    }
}
