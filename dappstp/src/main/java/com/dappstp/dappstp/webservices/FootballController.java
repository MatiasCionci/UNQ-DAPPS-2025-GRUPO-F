package com.dappstp.dappstp.webservices;

import com.dappstp.dappstp.service.getapifootball.FootballApiService;
import com.dappstp.dappstp.service.getapifootball.MatchesApiResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/football") // Ruta base para este controlador
public class FootballController {

    private final FootballApiService footballApiService;

    @Autowired
    public FootballController(FootballApiService footballApiService) {
        this.footballApiService = footballApiService;
    }

    @GetMapping("/matches")
    public ResponseEntity<MatchesApiResponseDto> getAllMatches() {
        MatchesApiResponseDto matchesResponse = footballApiService.getMatches("108");

        if (matchesResponse != null && matchesResponse.getMatches() != null && !matchesResponse.getMatches().isEmpty()) {
            return ResponseEntity.ok(matchesResponse);
        } else if (matchesResponse != null && (matchesResponse.getMatches() == null || matchesResponse.getMatches().isEmpty())) {
            return ResponseEntity.ok(matchesResponse); // Devuelve la respuesta aunque esté vacía, la API puede no tener partidos hoy
        } else {
            // Si footballApiService.getMatches() devolvió null (por un error en la llamada)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // O un DTO de error personalizado
        }
    }
}
