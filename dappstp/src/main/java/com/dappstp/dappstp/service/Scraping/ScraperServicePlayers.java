package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.repository.PlayerBarcelonaRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
// --- Imports de Selenium ELIMINADOS ---
// import org.openqa.selenium.*;
// import org.openqa.selenium.chrome.ChromeDriver;
// import org.openqa.selenium.chrome.ChromeOptions;
// import org.openqa.selenium.support.ui.ExpectedConditions;
// import org.openqa.selenium.support.ui.WebDriverWait;
// import org.openqa.selenium.PageLoadStrategy;
import org.springframework.stereotype.Service;

// --- Imports de Jsoup AÑADIDOS ---
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException; // Para manejar errores de conexión de Jsoup

// --- Imports estándar ---
// import java.io.FileOutputStream; // Ya no se necesita para screenshots
import java.time.Duration; // Se puede quitar si no se usa en otro lado
import java.util.ArrayList;
import java.util.List;
// import java.util.UUID; // Ya no se necesita

@Service
@Slf4j
public class ScraperServicePlayers { // INICIO CLASE

    private final PlayerBarcelonaRepository playerRepository;
    // private final String baseScreenshotPath = "/app/screenshot"; // Ya no se necesita

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() { // INICIO MÉTODO scrapeAndSavePlayers
        // ACTUALIZACIÓN v14: Usando Jsoup en lugar de Selenium.
        log.info("🚀 Iniciando scraping de jugadores del Barcelona (v14 - Jsoup)...");
        List<PlayerBarcelona> players = new ArrayList<>();
        String url = "https://es.whoscored.com/Teams/65/Show/Spain-Barcelona";
        // Timeout de conexión y lectura para Jsoup (en milisegundos)
        int jsoupTimeoutMillis = 60 * 1000; // 60 segundos

        try {
            log.info("Conectando a {} con Jsoup...", url);
            // Conectar y obtener el documento HTML
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36") // User agent genérico
                .timeout(jsoupTimeoutMillis)
                .get();
            log.info("Documento HTML obtenido.");

            // Buscar el cuerpo de la tabla por ID
            Element tbody = doc.getElementById("player-table-statistics-body");
            if (tbody == null) {
                log.error("¡ERROR CRÍTICO! No se encontró el elemento tbody con ID 'player-table-statistics-body' en el HTML.");
                // Opcional: Loguear parte del HTML para depurar
                // log.error("HTML recibido (parcial):\n{}", doc.body().html().substring(0, Math.min(doc.body().html().length(), 2000)));
                throw new RuntimeException("No se encontró la tabla de jugadores (tbody) en el HTML.");
            }
            log.debug("Elemento tbody encontrado.");

            // Seleccionar todas las filas (tr) dentro del tbody
            Elements rows = tbody.select("tr");
            log.info("Filas encontradas en tabla para procesar: {}", rows.size());

            if (rows.isEmpty()) {
                 log.warn("⚠️ La tabla inicial no contenía filas de jugadores.");
            }

            // Procesar cada fila
            for (int i = 0; i < rows.size(); i++) {
                Element row = rows.get(i);
                Elements cols = row.select("td"); // Seleccionar celdas (td)

                if (cols.size() < 15) {
                    log.trace("Fila {} omitida, columnas insuficientes: {}", i, cols.size());
                    continue;
                }

                try {
                    // Extracción de datos usando índices de columna
                    // Nombre (puede estar en varias líneas, tomar la segunda si existe)
                    String cell0Text = cols.get(0).text(); // Texto completo de la primera celda
                    String[] nameParts = cell0Text.split("\\n"); // Dividir por nueva línea si existe
                    String name = nameParts.length > 1 ? nameParts[1].trim() : nameParts[0].trim(); // Tomar segunda línea o la primera

                    if (name.isBlank()) {
                         log.warn("Fila {}: Nombre vacío detectado, fila omitida. Contenido celda[0]: {}", i, cell0Text);
                         continue;
                    }

                    String matches = cols.get(4).text().trim();
                    String goalsStr = cols.get(6).text(); // Obtener texto antes de parsear
                    String assistsStr = cols.get(7).text(); // Obtener texto antes de parsear
                    String ratingStr = cols.get(14).text(); // Obtener texto antes de parsear

                    // Crear y poblar el objeto PlayerBarcelona
                    PlayerBarcelona p = new PlayerBarcelona();
                    p.setName(name);
                    p.setMatches(matches); // Guardar como String
                    p.setGoals(parseIntSafe(goalsStr));
                    p.setAssists(parseIntSafe(assistsStr));
                    p.setRating(parseDoubleSafe(ratingStr));
                    players.add(p);
                    log.trace("Fila {}: Jugador procesado: {}", i, p.getName());

                } catch (Exception e) {
                    // Capturar cualquier error durante el parseo de una fila específica
                    log.error("Error procesando fila {}: {}. Contenido fila (aprox): {}", i, e.getMessage(), row.text().substring(0, Math.min(row.text().length(), 100)), e);
                    // Continuar con la siguiente fila
                }
            } // Fin del bucle for

            // Guardar jugadores encontrados
            if (!players.isEmpty()) {
                log.info("Guardando {} jugadores...", players.size());
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else if (!rows.isEmpty()) {
                // Si había filas pero ninguna resultó en un jugador válido
                log.warn("⚠️ No se procesaron jugadores válidos aunque se encontraron {} filas.", rows.size());
            }

        } catch (IOException e) {
            // Error de conexión/lectura de Jsoup
            log.error("¡ERROR CRÍTICO! Falló la conexión o lectura con Jsoup a {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Error al conectar o leer la URL con Jsoup.", e);
        } catch (Exception e) {
            // Otros errores inesperados
            log.error("Error general inesperado durante el scraping con Jsoup: {}", e.getMessage(), e);
            throw new RuntimeException("Error inesperado durante el scraping con Jsoup.", e);
        }
        // --- FINALLY block de Selenium eliminado ---

        log.info("🏁 Scraping finalizado. Total procesados: {} jugadores.", players.size());
        return players;
    } // FIN MÉTODO scrapeAndSavePlayers


    // --- Métodos auxiliares (Sin cambios, siguen siendo útiles) ---
    private int parseIntSafe(String txt) { // INICIO MÉTODO parseIntSafe
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0;
        try {
            String numPart = txt.split("\\(")[0].trim(); // Maneja "1(2)"
            String digits = numPart.replaceAll("[^\\d]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            log.warn("parseIntSafe falló para '{}': {}", txt, e.getMessage());
            return 0;
        }
    } // FIN MÉTODO parseIntSafe

    private double parseDoubleSafe(String txt) { // INICIO MÉTODO parseDoubleSafe
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0.0;
        try {
            String cleaned = txt.replace(",", "."); // Reemplazar coma decimal
            cleaned = cleaned.replaceAll("[^\\d.]", ""); // Quitar caracteres no numéricos excepto punto
            if (cleaned.isEmpty() || cleaned.equals(".")) return 0.0;
            // Asegurar un solo punto decimal
            int firstDot = cleaned.indexOf('.');
            if (firstDot != -1) {
                int secondDot = cleaned.indexOf('.', firstDot + 1);
                if (secondDot != -1) { cleaned = cleaned.substring(0, secondDot); }
            }
             if (cleaned.startsWith(".")) cleaned = "0" + cleaned; // Añadir 0 si empieza con .
             if (cleaned.endsWith(".")) cleaned = cleaned.substring(0, cleaned.length() - 1); // Quitar . al final
             if (cleaned.isEmpty()) return 0.0; // Re-verificar si quedó vacío
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
             log.warn("parseDoubleSafe falló para '{}' (NumberFormatException): {}", txt, e.getMessage());
             return 0.0;
        } catch (Exception e) {
            log.warn("parseDoubleSafe falló para '{}' (Exception): {}", txt, e.getMessage());
            return 0.0;
        }
    } // FIN MÉTODO parseDoubleSafe

    // --- Método takeScreenshot ELIMINADO (específico de Selenium) ---

} // FIN CLASE ScraperServicePlayers
