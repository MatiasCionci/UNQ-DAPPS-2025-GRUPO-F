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

        // Setup ChromeDriver automáticamente
        WebDriverManager.chromedriver().setup();

        String ua = USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
        log.debug("User-Agent seleccionado: {}", ua);

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NONE);
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=" + ua);
        options.addArguments("--user-data-dir=/tmp/chrome-profile-" + UUID.randomUUID());

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(180));
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            log.info("Navegando a {}", url);
            driver.get(url);

            // Click en Statistics
            By statsTab = By.xpath("//a[contains(text(),'Statistics')]");
            WebElement tab = wait.until(ExpectedConditions.elementToBeClickable(statsTab));
            tab.click();

            // Esperar filas
            wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.cssSelector("tbody#player-table-statistics-body tr"), 0));

            // Procesar tabla
            WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("player-table-statistics-body")));
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            log.info("Encontradas {} filas en la tabla.", rows.size());

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                log.trace("Fila con {} columnas.", cols.size());
                if (cols.isEmpty()) {
                    continue; // fila vacía o header
                }

                String name = extractName(cols.get(0));
                String matches = cols.size() > 4 ? cols.get(4).getText().trim() : "0";
                int goals = cols.size() > 6 ? parseIntSafe(cols.get(6).getText()) : 0;
                int assists = cols.size() > 7 ? parseIntSafe(cols.get(7).getText()) : 0;
                double rating;
                if (cols.size() > 14) {
                    rating = parseDoubleSafe(cols.get(14).getText());
                } else {
                    // rating en la última columna si índice 14 no existe
                    String txt = cols.get(cols.size() - 1).getText();
                    rating = parseDoubleSafe(txt);
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
            if (!txt.isEmpty()) {
                return txt;
            }
            return cell.findElement(By.cssSelector("a.player-link")).getText().trim();
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
