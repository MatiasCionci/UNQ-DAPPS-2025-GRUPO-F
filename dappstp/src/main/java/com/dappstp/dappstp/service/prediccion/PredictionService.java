package com.dappstp.dappstp.service.prediccion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.dappstp.dappstp.model.Prediction;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PredictionService {
    private final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private final String URL = "https://api.openai.com/v1/chat/completions";

    /**
     * Versión que recibe el resultado raw del scraping (ya limpio de HTML),
     * lo inserta tal cual en el prompt y pide un JSON de vuelta.
     */
    public Prediction predictFromRaw(String scrapedData) throws IOException, InterruptedException {
        // 1) Construyo el prompt incluyendo el bloque scrapedData
        String prompt = buildPromptFromRaw(scrapedData);

        // 2) Armo JSON de llamada a la API
        JSONObject message = new JSONObject()
            .put("role", "user")
            .put("content", prompt);
        JSONObject body = new JSONObject()
            .put("model", "gpt-4")
            .put("messages", new JSONArray().put(message))
            .put("temperature", 0.2);

        // 3) POST a OpenAI
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(URL))
            .header("Authorization", "Bearer " + OPENAI_API_KEY)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

        // 4) Parseo y devuelvo el Prediction
        String content = new JSONObject(response.body())
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content").trim();
        return new ObjectMapper().readValue(content, Prediction.class);
    }

    private String buildPromptFromRaw(String raw) {
        return """
            Eres un analista de fútbol. Te paso datos brutos de dos equipos (incluyendo H2H, forma, GF/GC, fortalezas, debilidades) en texto plano.
            Devuélveme sólo un JSON con estos campos:
              • winner: equipo ganador o "draw"
              • score: marcador en formato "X-Y"
              • confidence: confianza [0.0–1.0]

            Datos:
            """ 
            + raw + 
            """

            Contesta únicamente el JSON sin texto adicional.
            """;
    }
}
// Fin de la clase PredictionService
