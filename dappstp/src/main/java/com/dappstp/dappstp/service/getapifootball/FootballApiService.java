package com.dappstp.dappstp.service.getapifootball;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class FootballApiService {

    private static final Logger logger = LoggerFactory.getLogger(FootballApiService.class);
    private final RestTemplate restTemplate;
    @Value("${FOOTBALL_API_KEY}")
    private String apiKey;
  
    @Value("${FOOTBALL_API_KEY}")
    private String apiUrl;

    @Autowired
    public FootballApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
//86 es real madrid,historial de partidos de un enfrentamiento real madrid atletico madrid https://api.football-data.org/v4/matches/498684/head2head
    public MatchesApiResponseDto getMatches(String id) {
        String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .path("/teams/"+id+"/matches")
                // Aquí podrías agregar parámetros de consulta si la API los soporta/requiere
                // .queryParam("dateFrom", "YYYY-MM-DD")
                // .queryParam("dateTo", "YYYY-MM-DD")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", apiKey);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        try {
            logger.info("Consultando partidos desde football-data.org: {}", url);
            ResponseEntity<MatchesApiResponseDto> response = restTemplate.exchange(url, HttpMethod.GET, entity, MatchesApiResponseDto.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            logger.error("Error al consumir API de football-data: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            // Considera lanzar una excepción personalizada o devolver un Optional/null según tu estrategia de manejo de errores
            return null;
        }
    }
}