package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.repository.PlayersRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
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
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class ScraperServicePlayers {

    private final PlayersRepository playerRepository;
    private final Random random = new Random();

    private static final List<String> USER_AGENTS = List.of(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.5845.183 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64; rv:102.0) Gecko/20100101 Firefox/102.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/18.19041"
    );

    public ScraperServicePlayers(PlayersRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<Players> scrapeAndSavePlayers() {
        log.info("🚀 Iniciando scraping con WebDriverManager y user-agent rotativo...");
        List<Players> players = new ArrayList<>();

        // Setup driver
        WebDriverManager.chromedriver().setup();
        String ua = USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
        log.debug("User‑Agent seleccionado: {}", ua);

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NONE);
        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--user-agent=" + ua,
            "--user-data-dir=/tmp/chrome-profile-" + UUID.randomUUID()
        );

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(180));

            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            log.info("Navegando a {}", url);
            driver.get(url);

            // 1) Cerrar propaganda SweetAlert2 si aparece
            try {
                WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("div.webpush-swal2-shown")
                ));
                WebElement closeBtn = modal.findElement(By.cssSelector("button.webpush-swal2-close"));
                closeBtn.click();
                wait.until(ExpectedConditions.invisibilityOf(modal));
                log.debug("🎉 Popup cerrado.");
            } catch (TimeoutException | NoSuchElementException e) {
                log.debug("No apareció popup de propaganda.");
            }

            // 2) Seleccionar "All" en el select de filas
            try {
                WebElement lengthSelect = wait.until(
                    ExpectedConditions.elementToBeClickable(
                        By.cssSelector("select[name='player-table-statistics_length']")
                    )
                );
                new Select(lengthSelect).selectByVisibleText("All");
                // esperar recarga de tabla
                wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                    By.cssSelector("tbody#player-table-statistics-body tr"), 20
                ));
                log.debug("✅ 'All' seleccionado, al menos 21 filas cargadas.");
            } catch (Exception e) {
                log.warn("No se pudo cambiar número de filas: {}", e.getMessage());
            }

            // 3) Esperar la tabla y leer todas las filas
            WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("player-table-statistics-body")
            ));
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            log.info("🎯 Filas encontradas: {}", rows.size());

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.isEmpty()) {
                    continue;
                }
                String name    = extractName(cols.get(0));
                String matches = cols.size() > 4 ? cols.get(4).getText().trim() : "0";
                int goals      = cols.size() > 6 ? parseIntSafe(cols.get(6).getText()) : 0;
                int assists    = cols.size() > 7 ? parseIntSafe(cols.get(7).getText()) : 0;
                double rating;
                if (cols.size() > 14) {
                    rating = parseDoubleSafe(cols.get(14).getText());
                } else {
                    rating = parseDoubleSafe(cols.get(cols.size() - 1).getText());
                }

                Players p = new Players();
                p.setName(name);
                p.setMatches(matches);
                p.setGoals(goals);
                p.setAssists(assists);
                p.setRating(rating);
                players.add(p);
            }

            if (!players.isEmpty()) {
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else {
                log.warn("⚠️ No se procesaron jugadores.");
            }

        } catch (Exception e) {
            log.error("Error en scraping:", e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }

        return players;
    }

    private String extractName(WebElement cell) {
        try {
            WebElement span = cell.findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
            String txt = span.getText().trim();
            return txt.isEmpty()
                ? cell.findElement(By.cssSelector("a.player-link")).getText().trim()
                : txt;
        } catch (NoSuchElementException e) {
            String[] parts = cell.getText().split("\\R");
            return parts.length > 1 ? parts[1].trim() : parts[0].trim();
        }
    }

    private int parseIntSafe(String txt) {
        try {
            String num = txt.replaceAll("[^\\d]", "");
            return num.isEmpty() ? 0 : Integer.parseInt(num);
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String txt) {
        try {
            String clean = txt.replace(",", ".").replaceAll("[^\\d.]", "");
            int i = clean.indexOf('.');
            if (i != -1) {
                int j = clean.indexOf('.', i + 1);
                if (j != -1) {
                    clean = clean.substring(0, j);
                }
            }
            return clean.isEmpty() ? 0.0 : Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
    }
}