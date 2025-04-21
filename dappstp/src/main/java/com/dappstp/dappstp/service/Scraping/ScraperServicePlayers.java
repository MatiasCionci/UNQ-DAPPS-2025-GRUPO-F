package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.repository.PlayerBarcelonaRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
// Selenium imports eliminados
import org.springframework.stereotype.Service;

// Jsoup imports
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import org.jsoup.HttpStatusException; // <-- Importar HttpStatusException

// Otros imports
import java.util.ArrayList;
import java.util.List;
import java.util.Map; // <-- Añadir import para Map (cabeceras)
import java.util.HashMap; // <-- Añadir import para HashMap

@Service
@Slf4j
public class ScraperServicePlayers { // INICIO CLASE

    private final PlayerBarcelonaRepository playerRepository;

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() { // INICIO MÉTODO scrapeAndSavePlayers
        // ACTUALIZACIÓN v15: Jsoup con manejo de HttpStatusException y cabeceras.
        log.info("🚀 Iniciando scraping de jugadores del Barcelona (v15 - Jsoup with headers)...");
        List<PlayerBarcelona> players = new ArrayList<>();
        String url = "https://es.whoscored.com/Teams/65/Show/Spain-Barcelona";
        int jsoupTimeoutMillis = 60 * 1000; // 60 segundos

        // --- Definir Cabeceras HTTP ---
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"); // Un User-Agent común
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.put("Accept-Language", "es-ES,es;q=0.9,en;q=0.8"); // Preferir español
        // headers.put("Referer", "https://www.google.com/"); // Opcional: Simular venir de Google
        // headers.put("Accept-Encoding", "gzip, deflate, br"); // Jsoup maneja esto internamente

        try {
            log.info("Conectando a {} con Jsoup (con cabeceras)...", url);

            Document doc = Jsoup.connect(url)
                .headers(headers) // <-- Añadir cabeceras
                .timeout(jsoupTimeoutMillis)
                .get(); // <-- Esta línea (aprox 56) causaba el error

            log.info("Documento HTML obtenido con éxito (Código 2xx).");

            Element tbody = doc.getElementById("player-table-statistics-body");
            if (tbody == null) {
                log.error("¡ERROR CRÍTICO! No se encontró tbody 'player-table-statistics-body'.");
                throw new RuntimeException("No se encontró la tabla de jugadores (tbody) en el HTML.");
            }
            log.debug("Elemento tbody encontrado.");

            Elements rows = tbody.select("tr");
            log.info("Filas encontradas: {}", rows.size());

            if (rows.isEmpty()) {
                 log.warn("⚠️ La tabla no contenía filas.");
            }

            for (int i = 0; i < rows.size(); i++) {
                Element row = rows.get(i);
                Elements cols = row.select("td");
                if (cols.size() < 15) {
                    log.trace("Fila {} omitida, cols: {}", i, cols.size());
                    continue;
                }
                try {
                    String cell0Text = cols.get(0).text();
                    String[] nameParts = cell0Text.split("\\n");
                    String name = nameParts.length > 1 ? nameParts[1].trim() : nameParts[0].trim();
                    if (name.isBlank()) {
                         log.warn("Fila {}: Nombre vacío, omitida. Celda[0]: {}", i, cell0Text);
                         continue;
                    }
                    String matches = cols.get(4).text().trim();
                    String goalsStr = cols.get(6).text();
                    String assistsStr = cols.get(7).text();
                    String ratingStr = cols.get(14).text();

                    PlayerBarcelona p = new PlayerBarcelona();
                    p.setName(name);
                    p.setMatches(matches);
                    p.setGoals(parseIntSafe(goalsStr));
                    p.setAssists(parseIntSafe(assistsStr));
                    p.setRating(parseDoubleSafe(ratingStr));
                    players.add(p);
                    log.trace("Fila {}: Jugador procesado: {}", i, p.getName());
                } catch (Exception e) {
                    log.error("Error procesando fila {}: {}. Contenido fila (aprox): {}", i, e.getMessage(), row.text().substring(0, Math.min(row.text().length(), 100)), e);
                }
            }

            if (!players.isEmpty()) {
                log.info("Guardando {} jugadores...", players.size());
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else if (!rows.isEmpty()) {
                log.warn("⚠️ No se procesaron jugadores válidos aunque se encontraron {} filas.", rows.size());
            }

        // --- CAMBIO: Capturar HttpStatusException PRIMERO ---
        } catch (HttpStatusException e) {
            // Obtener y loguear el código de estado HTTP específico
            int statusCode = e.getStatusCode();
            String statusMessage = e.getMessage(); // Mensaje de error
            log.error("¡ERROR HTTP {} al conectar con Jsoup a {}: {}!", statusCode, url, statusMessage, e);
            log.error("Respuesta del servidor (parcial):\n{}", e.toString().substring(0, Math.min(e.toString().length(), 1000))); // Loguear parte de la respuesta si está en la excepción

            // Lanzar excepción para detener el proceso, indicando el código HTTP
            throw new RuntimeException("Error HTTP " + statusCode + " al obtener la URL con Jsoup. Posible bloqueo anti-scraping.", e);

        } catch (IOException e) {
            // Otros errores de red/conexión
            log.error("¡ERROR CRÍTICO de IO! Falló la conexión o lectura con Jsoup a {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Error de IO al conectar o leer la URL con Jsoup.", e);
        } catch (Exception e) {
            log.error("Error general inesperado durante el scraping con Jsoup: {}", e.getMessage(), e);
            throw new RuntimeException("Error inesperado durante el scraping con Jsoup.", e);
        }

        log.info("🏁 Scraping finalizado. Total procesados: {} jugadores.", players.size());
        return players;
    } // FIN MÉTODO scrapeAndSavePlayers


    // --- Métodos auxiliares (Sin cambios) ---
    private int parseIntSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0;
        try {
            String numPart = txt.split("\\(")[0].trim();
            String digits = numPart.replaceAll("[^\\d]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            log.warn("parseIntSafe falló para '{}': {}", txt, e.getMessage());
            return 0;
        }
    }

    private double parseDoubleSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0.0;
        try {
            String cleaned = txt.replace(",", ".");
            cleaned = cleaned.replaceAll("[^\\d.]", "");
            if (cleaned.isEmpty() || cleaned.equals(".")) return 0.0;
            int firstDot = cleaned.indexOf('.');
            if (firstDot != -1) {
                int secondDot = cleaned.indexOf('.', firstDot + 1);
                if (secondDot != -1) { cleaned = cleaned.substring(0, secondDot); }
            }
             if (cleaned.startsWith(".")) cleaned = "0" + cleaned;
             if (cleaned.endsWith(".")) cleaned = cleaned.substring(0, cleaned.length() - 1);
             if (cleaned.isEmpty()) return 0.0;
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
             log.warn("parseDoubleSafe falló para '{}' (NumberFormatException): {}", txt, e.getMessage());
             return 0.0;
        } catch (Exception e) {
            log.warn("parseDoubleSafe falló para '{}' (Exception): {}", txt, e.getMessage());
            return 0.0;
        }
    }

} // FIN CLASE ScraperServicePlayers
