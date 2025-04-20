package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.repository.PlayerBarcelonaRepository;
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

import org.openqa.selenium.PageLoadStrategy;

@Service
@Slf4j
public class ScraperServicePlayers {

    private final PlayerBarcelonaRepository playerRepository;

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() {
        log.info("🚀 Iniciando scraping...");
        List<PlayerBarcelona> players = new ArrayList<>();
        WebDriver driver = null;

        // --- Configuración de ChromeDriver ---
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
            " AppleWebKit/537.36 (KHTML, like Gecko)" +
            " Chrome/135.0.7049.95 Safari/537.36",
            "--user-data-dir=/tmp/chrome-profile-" + UUID.randomUUID()
        );

        try {
            log.debug("Inicializando ChromeDriver...");
            driver = new ChromeDriver(options);

            // 180 segundos de timeout para carga de página
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(180));

            // 30 segundos de timeout para esperas explícitas
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // 1. Navegar a la página
            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            log.info("Navegando a {}", url);
            driver.get(url);

            // 1.5 Obtener torneo seleccionado (solo debug)
            try {
                WebElement selectedOption = driver.findElement(
                    By.cssSelector("select[data-backbone-model-attribute-dd='tournamentOptions'] > option[selected]")
                );
                String torneoSeleccionado = selectedOption.getText().trim();
                log.debug("Torneo seleccionado: {}", torneoSeleccionado);
            } catch (NoSuchElementException e) {
                log.debug("No se encontró el torneo seleccionado.");
            }

            // 2. Cerrar SweetAlert (si aparece)
            try {
                By swalClose = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
                wait.until(ExpectedConditions.visibilityOfElementLocated(swalClose));
                WebElement btn = driver.findElement(swalClose);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("div.webpush-swal2-shown")));
            } catch (Exception ignored) {}

            // 3. Cerrar cookies (si aparece)
            try {
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                    By.cssSelector("iframe[title='SP Consent Message']"))
                );
                WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(., 'Accept') or contains(., 'Aceptar')]")
                ));
                try { btn.click(); }
                catch (Exception ex) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                }
                Thread.sleep(500);
            } catch (Exception ignored) {
            } finally {
                driver.switchTo().defaultContent();
            }

            // 4. Extraer tabla de jugadores
            WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("player-table-statistics-body")
            ));
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            log.info("Encontradas {} filas en la tabla.", rows.size());

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() < 15) continue;

                // Nombre
                String name;
                try {
                    WebElement span = cols.get(0)
                        .findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
                    name = span.getText().trim();
                    if (name.isEmpty()) {
                        name = cols.get(0)
                            .findElement(By.cssSelector("a.player-link"))
                            .getText().trim();
                    }
                } catch (NoSuchElementException e) {
                    String[] parts = cols.get(0).getText().split("\\n");
                    name = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                }

                // Estadísticas
                String matches = cols.get(4).getText().trim();
                int goals    = parseIntSafe(cols.get(6).getText());
                int assists  = parseIntSafe(cols.get(7).getText());
                double rating= parseDoubleSafe(cols.get(14).getText());

                // Crear y añadir el objeto
                PlayerBarcelona p = new PlayerBarcelona();
                p.setName(name);
                p.setMatches(matches);
                p.setGoals(goals);
                p.setAssists(assists);
                p.setRating(rating);
                players.add(p);
            }

            // 5. Guardar en BD
            if (!players.isEmpty()) {
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else {
                log.warn("⚠️ No se procesaron jugadores.");
            }

        } catch (Exception e) {
            log.error("Error general en scraping: ", e);
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
            log.warn("Error parseando int '{}': {}", txt, e.getMessage());
            return 0;
        }
    }

    private double parseDoubleSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0.0;
        try {
            String c = txt.replace(",", ".").replaceAll("[^\\d.]", "");
            int firstDot = c.indexOf('.');
            if (firstDot != -1) {
                int secondDot = c.indexOf('.', firstDot + 1);
                if (secondDot != -1) {
                    c = c.substring(0, secondDot);
                }
            }
            return Double.parseDouble(c);
        } catch (Exception e) {
            log.warn("Error parseando double '{}': {}", txt, e.getMessage());
            return 0.0;
        }
    }
}
