package com.dappstp.dappstp.webservices;

import com.dappstp.dappstp.config.ApiPaths; // Asumiendo que tienes ApiPaths
import com.dappstp.dappstp.service.getapifootball.FootballApiService;

import com.dappstp.dappstp.dto.MatchesApiResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dappstp.dappstp.webservices.dto.ErrorResponse; // Para respuestas de error

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(ApiPaths.API_BASE + "/v1/football") // Usando ApiPaths
@Tag(name = "Football Data API", description = "Endpoints para interactuar con la API externa de football-data.org.")
public class FootballController {

    private final FootballApiService footballApiService;

    public FootballController(FootballApiService footballApiService) {
        this.footballApiService = footballApiService;
    }

    @GetMapping("/matches")
    @Operation(summary = "Obtener partidos de un equipo (Inter por defecto)",
               description = "Consulta la API de football-data.org para obtener los partidos del equipo con ID 108 (Inter de Milán).")
    @ApiResponse(responseCode = "200", description = "Partidos obtenidos exitosamente.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchesApiResponseDto.class)))
    @ApiResponse(responseCode = "500", description = "Error al consultar la API externa.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<MatchesApiResponseDto> getAllMatches() {
        MatchesApiResponseDto matchesResponse = footballApiService.getMatches("108");

        if (matchesResponse != null && matchesResponse.getMatches() != null && !matchesResponse.getMatches().isEmpty()) {
            return ResponseEntity.ok(matchesResponse);
        } else if (matchesResponse != null && (matchesResponse.getMatches() == null || matchesResponse.getMatches().isEmpty())) {
            return ResponseEntity.ok(matchesResponse); // Devuelve la respuesta aunque esté vacía, la API puede no tener partidos hoy
        } else {
            // Si footballApiService.getMatches() devolvió null (por un error en la llamada)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MatchesApiResponseDto()); // O un DTO de error personalizado
        }
    }
}
