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
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/135.0.7049.95 Safari/537.36",
            "--user-data-dir=/tmp/chrome-profile-" + UUID.randomUUID()
        );

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(180));
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            driver.get(url);

            // ——— LÓGICA CLAVE: SELECCIÓN ACTIVA DE "LaLiga" ———
            try {
                Select torneoSelect = new Select(driver.findElement(
                    By.cssSelector("select[data-backbone-model-attribute-dd='tournamentOptions']")));
                torneoSelect.selectByVisibleText("LaLiga");
                // Esperar que recargue la tabla después de cambiar de torneo
                wait.until(ExpectedConditions.stalenessOf(
                    driver.findElement(By.id("player-table-statistics-body"))));
                log.debug("Seleccionado torneo LaLiga.");
            } catch (Exception e) {
                log.warn("No pude seleccionar LaLiga: {}", e.getMessage());
            }

            // Cerrar SweetAlert
            try {
                By swal = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
                wait.until(ExpectedConditions.elementToBeClickable(swal)).click();
                log.debug("SweetAlert cerrado.");
            } catch (Exception e) {
                log.debug("Sin SweetAlert activo.");
            }

            // Cerrar cookies
            try {
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                    By.cssSelector("iframe[title='SP Consent Message']")));
                By btn = By.xpath("//button[contains(., 'Accept') or contains(., 'Aceptar')]");
                WebElement accept = wait.until(ExpectedConditions.elementToBeClickable(btn));
                accept.click();
                log.debug("Banner de cookies cerrado.");
            } catch (Exception e) {
                log.debug("Sin banner de cookies.");
            } finally {
                driver.switchTo().defaultContent();
            }

            // Extraer tabla de jugadores
            WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("player-table-statistics-body")));
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            log.info("Tabla cargada con {} filas.", rows.size());

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() < 15) continue;

                String name;
                try {
                    name = cols.get(0)
                        .findElement(By.cssSelector("a.player-link span.iconize-icon-left"))
                        .getText().trim();
                    if (name.isEmpty()) {
                        name = cols.get(0).findElement(By.cssSelector("a.player-link")).getText().trim();
                    }
                } catch (Exception ex) {
                    String[] parts = cols.get(0).getText().split("\\n");
                    name = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                }

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
        try {
            String d = txt.split("\\(")[0].replaceAll("[^\\d]", "");
            return d.isEmpty() ? 0 : Integer.parseInt(d);
        } catch (Exception e) {
            log.warn("parseInt: '{}'", txt);
            return 0;
        }
    }

    private double parseDoubleSafe(String txt) {
        try {
            String c = txt.replace(",", ".").replaceAll("[^\\d.]", "");
            int dot = c.indexOf('.');
            if (dot != -1 && c.indexOf('.', dot+1) != -1) {
                c = c.substring(0, c.indexOf('.', dot+1));
            }
            return c.isEmpty() ? 0.0 : Double.parseDouble(c);
        } catch (Exception e) {
            log.warn("parseDouble: '{}'", txt);
            return 0.0;
        }
    }
}
