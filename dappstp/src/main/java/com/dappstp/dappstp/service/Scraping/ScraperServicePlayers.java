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
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/136.0.7103.59 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64; rv:102.0) Gecko/20100101 Firefox/102.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Edge/18.19041"
    );

    public ScraperServicePlayers(PlayersRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<Players> scrapeAndSavePlayers() {
        log.info("🚀 Iniciando scraping con WebDriverManager y user‑agent rotativo...");
        List<Players> players = new ArrayList<>();

        // 1) Setup driver
        WebDriverManager.chromedriver().setup();
        String ua = USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
        ChromeOptions opts = new ChromeOptions();
        opts.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        String userDataDir = "/app/chrome-user-data-" + UUID.randomUUID();
        opts.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--user-agent=" + ua,
            "--user-data-dir=" + userDataDir,
            "--remote-allow-origins=*"
        );

        WebDriver driver = new ChromeDriver(opts);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

        try {
            // 2) Navegar a la página
            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            log.info("🌐 Navegando a {}", url);
            driver.get(url);

            // 3) Cerrar popup de suscripción si aparece
            try {
                By popupClose = By.cssSelector("button.webpush-swal2-close");
                wait.until(ExpectedConditions.elementToBeClickable(popupClose)).click();
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.swal2-container")));
            } catch (TimeoutException ignored) {
            }

            // 4) Seleccionar pestaña “En general” (Overall)
            try {
                By overallTab = By.cssSelector("a.option[data-value='Overall']");
                log.debug("Esperando pestaña 'En general'…");
                wait.until(ExpectedConditions.elementToBeClickable(overallTab)).click();
                log.debug("Pestaña 'En general' clickeada.");
                Thread.sleep(1_000);  // dar tiempo a recargar la tabla
            } catch (Exception e) {
                log.warn("No se pudo clicar 'En general', puede que ya estuviera seleccionado.", e);
            }

            // 5) Esperar a que la tabla cargue sus filas
            By rowsLocator = By.cssSelector("tbody#player-table-statistics-body tr");
            wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(rowsLocator, 0));
            List<WebElement> rows = driver.findElements(rowsLocator);
            log.info("🎯 Filas encontradas: {}", rows.size());

            // 6) Extraer datos de cada fila
            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() < 5) continue;

                String name    = extractName(cols.get(0));
                String matches = cols.get(4).getText().trim();
                int    goals   = parseIntSafe(cols.get(6).getText());
                int    assists = parseIntSafe(cols.get(7).getText());
                double rating  = parseDoubleSafe(
                    cols.size() > 14 ? cols.get(14).getText() 
                                     : cols.get(cols.size() - 1).getText()
                );

                Players p = new Players();
                p.setName(name);
                p.setMatches(matches);
                p.setGoals(goals);
                p.setAssists(assists);
                p.setRating(rating);
                players.add(p);
            }

            // 7) Guardar en la base si hay resultados
            if (!players.isEmpty()) {
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else {
                log.warn("⚠️ No se procesaron jugadores.");
            }

        } catch (Exception e) {
            log.error("Error en scraping:", e);
        } finally {
            driver.quit();
        }

        return players;
    }

    private String extractName(WebElement cell) {
        try {
            WebElement span = cell.findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
            String t = span.getText().trim();
            if (!t.isBlank()) return t;
            return cell.findElement(By.cssSelector("a.player-link")).getText().trim();
        } catch (NoSuchElementException ex) {
            String[] parts = cell.getText().split("\\R");
            return parts.length > 1 ? parts[1].trim() : parts[0].trim();
        }
    }

    private int parseIntSafe(String txt) {
        String n = txt.replaceAll("[^0-9]", "");
        return n.isEmpty() ? 0 : Integer.parseInt(n);
    }

    private double parseDoubleSafe(String txt) {
        String clean = txt.replace(",", ".").replaceAll("[^0-9.]", "");
        int i = clean.indexOf('.');
        if (i >= 0) {
            int j = clean.indexOf('.', i + 1);
            if (j > 0) clean = clean.substring(0, j);
        }
        return clean.isEmpty() ? 0.0 : Double.parseDouble(clean);
    }
}
