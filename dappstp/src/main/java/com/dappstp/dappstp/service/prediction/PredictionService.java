package com.dappstp.dappstp.service.prediction;

import com.dappstp.dappstp.model.Prediction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

import java.util.Collections;

@Service
public class PredictionService {

    private static final Logger logger = LoggerFactory.getLogger(PredictionService.class);
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";


    private final RestTemplate restTemplate ;
    private final ObjectMapper objectMapper = new ObjectMapper();

        public PredictionService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10)) // Timeout para establecer la conexión
                .setReadTimeout(Duration.ofSeconds(30))    // Timeout para leer la respuesta
                .build();
    }
    public Prediction analyzeMatch(String scrapedData) {
        logger.debug("Iniciando análisis para datos: {}", scrapedData);
        
        try {
            String prompt = buildPrompt(scrapedData);
            logger.debug("Prompt construido: {}", prompt);
            
            String response = getGeminiResponse(prompt);
            logger.debug("Respuesta cruda recibida: {}", response);
            
            return parseGeminiResponse(response);
        } catch (Exception e) {
            logger.error("Error en el análisis: {}", e.getMessage());
            throw new RuntimeException("Error procesando predicción: " + e.getMessage());
        }
    }

    private String buildPrompt(String data) {
        return String.format("""
            Eres un analista de fútbol profesional. Genera un JSON válido con esta estructura:
            {
              "winner": "Nombre del equipo",
              "confidence": 0.95,
              "reasons": ["Razón clave"],
              "scorePrediction": "X-X"
            }
            Analiza estos datos: %s
            
            Reglas:
            1. El JSON debe ser válido y sin texto adicional
            2. Usa máximo 1 razón clave
            3. La predicción debe ser realista
            """, data.replace("\"", "\\\""));
    }

    private String getGeminiResponse(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            JSONObject requestBody = new JSONObject()
                .put("contents", Collections.singletonList(
                    new JSONObject()
                        .put("parts", Collections.singletonList(
                            new JSONObject().put("text", prompt)
                        ))
                ));

            String url = GEMINI_URL + "?key=" + apiKey;
            
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new HttpClientErrorException(response.getStatusCode(), 
                    "Error en Gemini API: " + response.getBody());
            }
            
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            String errorMsg = String.format("Error HTTP %d: %s", 
                e.getStatusCode().value(), e.getResponseBodyAsString());
            logger.error(errorMsg);
            throw new RuntimeException("Error de comunicación con Gemini API");
        } catch (Exception e) {
            logger.error("Error inesperado: {}", e.getMessage());
            throw new RuntimeException("Error interno del servidor");
        }
    }

    private Prediction parseGeminiResponse(String jsonResponse) throws JsonProcessingException {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode candidate = rootNode.path("candidates").get(0);
            String generatedText = candidate.path("content").path("parts").get(0).path("text").asText();
            
            // Extraer solo el JSON válido
            int jsonStart = generatedText.indexOf('{');
            int jsonEnd = generatedText.lastIndexOf('}') + 1;
            String jsonData = generatedText.substring(jsonStart, jsonEnd);
            
            // Validar estructura básica
            if (!jsonData.contains("\"winner\"") || !jsonData.contains("\"confidence\"")) {
                throw new IllegalArgumentException("Estructura JSON inválida en respuesta");
            }
            
            return objectMapper.readValue(jsonData, Prediction.class);
            
        } catch (Exception e) {
            logger.error("Respuesta inválida: {}\nError: {}", jsonResponse, e.getMessage());
            throw new JsonProcessingException("Error procesando respuesta de Gemini") {};
        }
    }
}