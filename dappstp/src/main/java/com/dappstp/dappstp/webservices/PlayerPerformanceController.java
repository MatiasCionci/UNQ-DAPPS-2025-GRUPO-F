package com.dappstp.dappstp.webservices;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dappstp.dappstp.model.PerformanceAnalysis;
import com.dappstp.dappstp.service.PlayerPerformanceService;

@RestController
@RequestMapping("/api/players")
public class PlayerPerformanceController {

    private final PlayerPerformanceService performanceService;

    public PlayerPerformanceController(PlayerPerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    /**
     * GET /api/players/{playerId}/performance
     */
    @GetMapping("/{playerId}/performance")
    public ResponseEntity<PerformanceAnalysis> getPlayerPerformance(
            @PathVariable("playerId") String playerId) {

        PerformanceAnalysis analysis = performanceService.analyzePlayerPerformance(playerId);
        return ResponseEntity.ok(analysis);
    }
}
