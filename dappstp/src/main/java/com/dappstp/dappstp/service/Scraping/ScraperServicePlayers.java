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
import org.openqa.selenium.PageLoadStrategy;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ScraperServicePlayers {

    private final PlayerBarcelonaRepository playerRepository;

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() {
        List<PlayerBarcelona> players = new ArrayList<>();
        WebDriver driver = null;
        String baseScreenshotPath = "/app/screenshot";

        ChromeOptions options = new ChromeOptions();
        // Ruta al binario de Chrome en Render
        options.setBinary("/usr/bin/google-chrome-stable");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments(
            "--headless",
            "--no-sandbox",
            "--disable-setuid-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--remote-debugging-port=9222",
            "--window-size=1920,1080",
            "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/135.0.7049.95 Safari/537.36",
            "--user-data-dir=/tmp/chrome-profile-" + UUID.randomUUID()
        );

        try {
            log.info("🚀 Iniciando scraping de jugadores del Barcelona...");
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(180));
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

            // Navegar a la página
            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            log.info("Navegando a {}", url);
            driver.get(url);

            // Cerrar SweetAlert si existe
            try {
                By swalClose = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
                wait.until(ExpectedConditions.visibilityOfElementLocated(swalClose));
                WebElement btn = driver.findElement(swalClose);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.webpush-swal2-shown")));
                log.info("SweetAlert cerrado.");
            } catch (Exception e) {
                log.debug("No se encontró SweetAlert o fallo al cerrarlo: {}", e.getMessage());
            }

            // Cerrar banner de cookies si existe
            try {
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                    By.cssSelector("iframe[title='SP Consent Message']")));
                By acceptBtn = By.xpath("//button[contains(., 'Accept') or contains(., 'Aceptar')]");
                WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(acceptBtn));
                try {
                    btn.click();
                } catch (ElementClickInterceptedException ex) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                }
                log.info("Banner de cookies cerrado.");
                Thread.sleep(500);
            } catch (Exception e) {
                log.debug("No se encontró banner de cookies o fallo al cerrarlo: {}", e.getMessage());
            } finally {
                driver.switchTo().defaultContent();
            }

            // Selección de LaLiga en el dropdown
            By torneoLocator = By.cssSelector("select[data-backbone-model-attribute-dd='tournamentOptions']");
            WebElement oldTable = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("player-table-statistics-body"))
            );
            try {
                WebElement selectElem = wait.until(ExpectedConditions.elementToBeClickable(torneoLocator));
                new Select(selectElem).selectByVisibleText("LaLiga");
                log.info("Seleccionado torneo LaLiga.");

                wait.until(ExpectedConditions.stalenessOf(oldTable));
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("player-table-statistics-body")));
                log.info("Tabla actualizada para LaLiga.");
            } catch (Exception e) {
                log.error("Error seleccionando LaLiga: {}", e.getMessage());
                takeScreenshot(driver, baseScreenshotPath + "_lali.png");
                throw e;
            }

            // Extracción de jugadores
            List<WebElement> rows = driver.findElements(By.cssSelector("#player-table-statistics-body tr"));
            log.info("Filas encontradas: {}", rows.size());

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() < 15) continue;

                String name;
                try {
                    name = cols.get(0).findElement(By.cssSelector("a.player-link span.iconize-icon-left")).getText().trim();
                    if (name.isEmpty()) {
                        name = cols.get(0).findElement(By.cssSelector("a.player-link")).getText().trim();
                    }
                } catch (NoSuchElementException ex) {
                    String[] parts = cols.get(0).getText().split("\\n");
                    name = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                }
                if (name.isBlank()) continue;

                PlayerBarcelona p = new PlayerBarcelona();
                p.setName(name);
                p.setMatches(cols.get(4).getText().trim());
                p.setGoals(parseIntSafe(cols.get(6).getText()));
                p.setAssists(parseIntSafe(cols.get(7).getText()));
                p.setRating(parseDoubleSafe(cols.get(14).getText()));
                players.add(p);
            }

            if (!players.isEmpty()) {
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else {
                log.warn("⚠️ No se procesaron jugadores.");
            }

        } catch (SessionNotCreatedException e) {
            log.error("No se pudo iniciar nueva sesión de Chrome: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error general en scraping: {}", e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }

        log.info("🏁 Scraping finalizado. Total: {} jugadores.", players.size());
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
            String cleaned = txt.replace(",", ".").replaceAll("[^\\d.]", "");
            if (cleaned.isEmpty() || cleaned.equals(".")) return 0.0;
            int firstDot = cleaned.indexOf('.');
            if (firstDot != -1) {
                int secondDot = cleaned.indexOf('.', firstDot + 1);
                if (secondDot != -1) cleaned = cleaned.substring(0, secondDot);
            }
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            log.warn("parseDoubleSafe falló para '{}': {}", txt, e.getMessage());
            return 0.0;
        }
    }

    private void takeScreenshot(WebDriver driver, String path) {
        if (!(driver instanceof TakesScreenshot)) return;
        try {
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            try (FileOutputStream fos = new FileOutputStream(path)) {
                fos.write(bytes);
                log.warn("Captura guardada en: {}", path);
            }
        } catch (Exception e) {
            log.error("No se pudo guardar screenshot: {}", e.getMessage());
        }
    }
}
