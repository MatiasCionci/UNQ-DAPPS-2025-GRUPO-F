package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.repository.PlayerBarcelonaRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ScraperServicePlayers {

    private final PlayerBarcelonaRepository playerRepository;

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() {
        log.info("🚀 Iniciando scraping...");
        List<PlayerBarcelona> players = new ArrayList<>();
        ChromeOptions options = new ChromeOptions();
        // Esperar carga completa de la página (página, scripts, etc.)
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--user-data-dir=/tmp/chrome-profile-" + UUID.randomUUID(),
            "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/135.0.7049.95 Safari/537.36",
            // Opciones opcionales anti-detección, activar si es necesario:
            // "--disable-blink-features=AutomationControlled"
        );

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            // Timeout para carga de página completa
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(180));
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            log.info("Navegando a {}", url);
            driver.get(url);

            // —— Esperar y seleccionar "LaLiga" ——
            By torneoLocator = By.cssSelector(
                "select[data-backbone-model-attribute-dd='tournamentOptions']"
            );
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(torneoLocator));
                Select torneoSelect = new Select(driver.findElement(torneoLocator));
                torneoSelect.selectByVisibleText("LaLiga");
                log.debug("Seleccionado torneo LaLiga");
                // Esperar recarga de la tabla tras cambio de torneo
                wait.until(ExpectedConditions.stalenessOf(
                    driver.findElement(By.id("player-table-statistics-body"))
                ));
            } catch (TimeoutException | NoSuchElementException e) {
                log.warn("No se pudo seleccionar LaLiga: {}", e.getMessage());
            }

            // —— Cerrar SweetAlert si aparece ——
            try {
                By swalClose = By.cssSelector(
                    "div.webpush-swal2-shown button.webpush-swal2-close"
                );
                wait.until(ExpectedConditions.elementToBeClickable(swalClose)).click();
                log.debug("SweetAlert cerrado");
            } catch (Exception e) {
                log.debug("Sin SweetAlert activo");
            }

            // —— Cerrar banner de cookies si aparece ——
            try {
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                    By.cssSelector("iframe[title='SP Consent Message']")
                ));
                By acceptBtn = By.xpath(
                    "//button[contains(., 'Accept') or contains(., 'Aceptar')]"
                );
                wait.until(ExpectedConditions.elementToBeClickable(acceptBtn)).click();
                log.debug("Banner de cookies cerrado");
            } catch (Exception e) {
                log.debug("Sin banner de cookies");
            } finally {
                driver.switchTo().defaultContent();
            }

            // —— Extraer y procesar tabla de jugadores ——
            WebElement table = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.id("player-table-statistics-body")
                )
            );
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            log.info("Tabla cargada con {} filas", rows.size());

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() < 15) continue;

                String name;
                try {
                    name = cols.get(0)
                        .findElement(By.cssSelector("a.player-link span.iconize-icon-left"))
                        .getText().trim();
                    if (name.isEmpty()) {
                        name = cols.get(0)
                            .findElement(By.cssSelector("a.player-link"))
                            .getText().trim();
                    }
                } catch (NoSuchElementException ex) {
                    String[] parts = cols.get(0).getText().split("\\n");
                    name = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                }

                PlayerBarcelona player = new PlayerBarcelona();
                player.setName(name);
                player.setMatches(cols.get(4).getText().trim());
                player.setGoals(parseIntSafe(cols.get(6).getText()));
                player.setAssists(parseIntSafe(cols.get(7).getText()));
                player.setRating(parseDoubleSafe(cols.get(14).getText()));
                players.add(player);
            }

            if (!players.isEmpty()) {
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else {
                log.warn("⚠️ No se procesaron jugadores.");
            }

        } catch (Exception e) {
            log.error("Error general en scraping", e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }

        return players;
    }

    private int parseIntSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0;
        try {
            String d = txt.split("\\(")[0].replaceAll("[^\\d]", "");
            return d.isEmpty() ? 0 : Integer.parseInt(d);
        } catch (Exception e) {
            log.warn("parseIntSafe falló para '{}': {}", txt, e.getMessage());
            return 0;
        }
    }

    private double parseDoubleSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0.0;
        try {
            String c = txt.replace(",", ".").replaceAll("[^\\d.]", "");
            int dot = c.indexOf('.');
            if (dot != -1 && c.indexOf('.', dot + 1) != -1) {
                c = c.substring(0, c.indexOf('.', dot + 1));
            }
            return c.isEmpty() ? 0.0 : Double.parseDouble(c);
        } catch (Exception e) {
            log.warn("parseDoubleSafe falló para '{}': {}", txt, e.getMessage());
            return 0.0;
        }
    }
}
