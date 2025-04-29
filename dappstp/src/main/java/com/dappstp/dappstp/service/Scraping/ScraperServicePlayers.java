package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.repository.PlayersRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
// Asegúrate de tener esta importación si no estaba ya
import org.openqa.selenium.PageLoadStrategy;

@Service
@Slf4j
public class ScraperServicePlayers {

    private final PlayersRepository playerRepository;

    public ScraperServicePlayers(PlayersRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<Players> scrapeAndSavePlayers() {
        log.info("🚀 Iniciando scraping...");
        List<Players> players = new ArrayList<>();
        WebDriver driver = null;

        ChromeOptions options = new ChromeOptions();
        // --- CAMBIO PRINCIPAL AQUÍ ---
        // Cambiado de NORMAL a EAGER para que driver.get() no espere la carga completa (scripts, imágenes, etc.)
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        // ---------------------------
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox"); // Necesario si corre como root en Docker
        options.addArguments("--disable-dev-shm-usage"); // Mitiga problemas con /dev/shm limitado en Docker
        options.addArguments("--disable-gpu"); // A menudo innecesario y puede ahorrar recursos en headless
        options.addArguments("--window-size=1920,1080"); // Define un tamaño de ventana
        // User agent para simular un navegador común
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
            + " AppleWebKit/537.36 (KHTML, like Gecko)"
            + " Chrome/135.0.7049.95 Safari/537.36");
        // Directorio de datos de usuario temporal para evitar conflictos
        String userDataDir = "/tmp/chrome-profile-" + UUID.randomUUID();
        options.addArguments("--user-data-dir=" + userDataDir);
       // Opciones anti-detección (comentadas por ahora, pueden reactivarse si es necesario)
        options.addArguments("--disable-blink-features=AutomationControlled");
       options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        try {
            log.debug("Inicializando ChromeDriver...");
            driver = new ChromeDriver(options);
            // Timeout para carga de página (menos crítico con EAGER, pero se mantiene)
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(180));
            // Timeout para esperas explícitas (WebDriverWait)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(180));

            // 1. Navegar a la página
            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            log.info("Navegando a {}", url);
            driver.get(url); // Con EAGER, esto debería retornar más rápido

    

            // 4. Extraer tabla (esperar a que sea visible)
            log.debug("Esperando la tabla de jugadores...");
            WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("player-table-statistics-body")));
            log.debug("Tabla encontrada. Extrayendo filas...");
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            log.info("Encontradas {} filas en la tabla.", rows.size());

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() < 15) {
                    log.trace("Fila omitida, columnas insuficientes: {}", cols.size());
                    continue;
                }

                String name;
                try {
                    WebElement span = cols.get(0)
                        .findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
                    name = span.getText().trim();
                    if (name.isEmpty()) {
                        name = cols.get(0).findElement(By.cssSelector("a.player-link")).getText().trim();
                        log.trace("Nombre obtenido por fallback de link: {}", name);
                    }
                } catch (NoSuchElementException e) {
                    String[] parts = cols.get(0).getText().split("\\n");
                    name = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                    log.trace("Nombre obtenido por fallback de texto directo: {}", name);
                }

                String matches = cols.get(4).getText().trim();
                int goals    = parseIntSafe(cols.get(6).getText());
                int assists  = parseIntSafe(cols.get(7).getText());
                double rating= parseDoubleSafe(cols.get(14).getText());

                Players p = new Players();
                p.setName(name);
                p.setMatches(matches);
                p.setGoals(goals);
                p.setAssists(assists);
                p.setRating(rating);
                players.add(p);
                log.trace("Jugador procesado: {}", name);
            }

            if (!players.isEmpty()) {
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else {
                log.warn("⚠️ No se procesaron jugadores de la tabla.");
            }
        } catch (TimeoutException e) {
             log.error("Timeout esperando un elemento específico (WebDriverWait): {}", e.getMessage());
        }
        catch (Exception e) {
            // Captura errores generales, incluyendo posibles timeouts de pageLoad si aún ocurren
            log.error("Error general en scraping (podría ser pageLoad timeout u otro): ", e);
        } finally {
            if (driver != null) {
                log.info("Cerrando WebDriver.");
                driver.quit();
            }
        }

        return players;
    }

    private int parseIntSafe(String txt) {
        if (txt==null||txt.isBlank()||txt.equals("-")) return 0;
        try {
            String d = txt.split("\\(")[0].replaceAll("[^\\d]", "");
            return d.isEmpty() ? 0 : Integer.parseInt(d);
        } catch (Exception e) {
            log.warn("Error parseando int: '{}'", txt, e);
            return 0;
        }
    }

    private double parseDoubleSafe(String txt) {
        if (txt==null||txt.isBlank()||txt.equals("-")) return 0.0;
        try {
            String c = txt.replace(",",".").replaceAll("[^\\d.]", "");
            if (c.isEmpty()) return 0.0;
            int firstDot = c.indexOf('.');
            if (firstDot != -1) {
                int secondDot = c.indexOf('.', firstDot + 1);
                if (secondDot != -1) {
                    c = c.substring(0, secondDot);
                }
            }
            return Double.parseDouble(c);
        } catch (Exception e) {
            log.warn("Error parseando double: '{}'", txt, e);
            return 0.0;
        }
    }
}
