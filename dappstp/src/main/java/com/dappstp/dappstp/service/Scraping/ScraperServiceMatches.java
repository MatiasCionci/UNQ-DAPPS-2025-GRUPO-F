package com.dappstp.dappstp.service.scraping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dappstp.dappstp.model.MatchStatus;
import com.dappstp.dappstp.model.UpcomingMatch;
import com.dappstp.dappstp.repository.UpcomingMatchRepository;

import jakarta.transaction.Transactional;

import io.github.bonigarcia.wdm.WebDriverManager;

import com.dappstp.dappstp.model.UpcomingMatch;
import com.dappstp.dappstp.model.MatchStatus;
import com.dappstp.dappstp.model.Team;
import com.dappstp.dappstp.repository.TeamRepository;
import com.dappstp.dappstp.repository.UpcomingMatchRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ScraperServiceMatches {
   private final UpcomingMatchRepository repo;

    private static final Logger log = LoggerFactory.getLogger(ScraperServiceMatches.class);
    private final UpcomingMatchRepository repo;
    private final TeamRepository teamRepo;

    public ScraperServiceMatches(UpcomingMatchRepository repo, TeamRepository teamRepo) {
        this.repo = repo;
        this.teamRepo = teamRepo;
    }

   public Team scrapeAndSave(String whoscoredUrl) {
        // Aseguramos la ruta /fixtures
        String url = whoscoredUrl;
        if (!url.contains("/fixtures")) {
            url = url.replaceAll("/teams/(\\d+)(/.*)?$", "/teams/$1/fixtures");
        }
        log.debug("Scraping Team header en URL: {}", url);

        // Arrancamos Selenium
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments(
            "--headless=new", "--no-sandbox", "--disable-gpu", "--window-size=1920,1080",
            "--disable-blink-features=AutomationControlled", "--disable-extensions",
            "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
        );
        opts.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        WebDriver driver = new ChromeDriver(opts);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        try {
            driver.get(url);

            // esperamos que el DOM se cargue
            wait.until(d -> ((JavascriptExecutor) d)
                .executeScript("return document.readyState").equals("complete"));

            // 1) Intentamos selectores específicos
            WebElement headerElt = null;
            try {
                headerElt = driver.findElement(By.cssSelector("header.team-header h1"));
            } catch (NoSuchElementException ignored) {}
            if (headerElt == null) {
                try {
                    headerElt = driver.findElement(By.cssSelector("div.team-header h1"));
                } catch (NoSuchElementException ignored) {}
            }
            // 2) Fallback: el primer <h1> del documento
            if (headerElt == null) {
                headerElt = driver.findElement(By.tagName("h1"));
            }

            if (headerElt == null || headerElt.getText().isBlank()) {
                throw new IllegalStateException(
                    "No encontré el <h1> de nombre de equipo en " + url);
            }

            String name = headerElt.getText().trim();
            Team team = new Team();
            team.setName(name);
            team.setWhoscoredUrl(url);
            return teamRepo.save(team);

        } catch (Exception e) {
            throw new IllegalStateException(
                "Error extrayendo <h1> en " + url + ": " + e.getMessage(), e);
        } finally {
            driver.quit();
        }
    }


    @Transactional
    public List<UpcomingMatch> scrapeAndSync(Long teamId, String url) {
        // idéntica a antes: llama a scrapeUpcomingMatches, setea teamId/status, sincroniza con BD…
        // …
        return repo.saveAll(
            scrapeUpcomingMatches(url).stream()
                .peek(m -> m.setTeamId(teamId))
                .peek(m -> m.setStatus(MatchStatus.PENDING))
                .toList()
        );
    }

    public List<UpcomingMatch> scrapeUpcomingMatches(String url) {
        // 0) Setup WebDriver  
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-blink-features=AutomationControlled",        // evita detección
            "--disable-extensions",
            "--disable-infobars"
        );
        // opcional: override del user-agent
        opts.addArguments(
            "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
        );
        // fuerza pageLoadStrategy a NORMAL
        opts.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        WebDriver driver = new ChromeDriver(opts);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        List<UpcomingMatch> matches = new ArrayList<>();

        try {
            // 1) Navegar y aguardar a que document.readyState sea 'complete'
            driver.get(url);
            wait.until(webDriver ->
                ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState")
                    .equals("complete")
            );

            // 2) Desplazar un poco hacia abajo para forzar cargas lazy/XHR
            ((JavascriptExecutor) driver).executeScript(
                "window.scrollBy(0, document.body.scrollHeight / 4);"
            );
            Thread.sleep(2000);

            // 3) (Opcional) click en pestaña "Fixtures" si existe
            try {
                WebElement fixturesTab = wait.until(
                    ExpectedConditions.elementToBeClickable(
                        By.xpath("//a[contains(text(),'Fixtures')]")
                    )
                );
                fixturesTab.click();
                // nueva carga
                    wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState")
                        .equals("complete")
                );
            } catch (TimeoutException ignored) {}

            // 4) Ahora SÍ esperamos a que aparezca el div-table dinámico
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("team-fixtures")
            ));
            // y al menos una fila
            wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.cssSelector("#team-fixtures .divtable-body .divtable-row"),
                0
            ));

            // 5) Parsear filas
            List<WebElement> rows = driver.findElements(
                By.cssSelector("#team-fixtures .divtable-body .divtable-row")
            );
            for (WebElement row : rows) {
                try {
                    List<WebElement> cols = row.findElements(
                        By.cssSelector("div[class*='col12']")
                    );
                    String utc = row.getAttribute("data-id");  // normalmente timestamp
                    long ms   = Long.parseLong(utc);
                    LocalDateTime kickoff = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(ms), ZoneId.of("UTC")
                    );
                    String competition = cols.get(1).getText().trim();
                    String home        = cols.get(3).getText().trim();
                    String away        = cols.get(5).getText().trim();

                    UpcomingMatch m = new UpcomingMatch();
                    m.setKickoff(kickoff);
                    m.setCompetition(competition);
                    m.setHomeTeam(home);
                    m.setAwayTeam(away);
                    m.setVenue("");
                    matches.add(m);
                } catch (Exception ignored) {
                    // no rompe el bucle
                }
            }

        } catch (Exception e) {
            log.error("Error inesperado en scraping de {}: {}", url, e.toString());
        } finally {
            driver.quit();
        }
        return matches;
    }


}


