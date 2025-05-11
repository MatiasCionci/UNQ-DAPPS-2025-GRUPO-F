package com.dappstp.dappstp.service.scraping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import io.github.bonigarcia.wdm.WebDriverManager;



@Service
public class ScraperServiceMatches {
/**
 * 
 *     private final UpcomingMatchRepository repo;

    public ScraperServiceMatches(UpcomingMatchRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public List<UpcomingMatch> scrapeAndSync(Long teamId, String url) {
        LocalDateTime now = LocalDateTime.now();

        List<UpcomingMatch> scraped = scrapeUpcomingMatches(url)
            .stream()
            .peek(m -> m.setTeamId(teamId))
            .peek(m -> m.setStatus(MatchStatus.PENDING))
            .collect(Collectors.toList());

        List<UpcomingMatch> existing = repo.findByTeamIdAndStatus(teamId, MatchStatus.PENDING);

        Map<String, UpcomingMatch> keyToExisting = existing.stream()
            .collect(Collectors.toMap(
                m -> m.getHomeTeam() + "|" + m.getAwayTeam() + "|" + m.getKickoff(),
                m -> m
            ));

        List<UpcomingMatch> toSave = new ArrayList<>();

        for (UpcomingMatch sc : scraped) {
            String key = sc.getHomeTeam() + "|" + sc.getAwayTeam() + "|" + sc.getKickoff();
            if (keyToExisting.containsKey(key)) {
                UpcomingMatch ex = keyToExisting.get(key);
                if (!Objects.equals(ex.getVenue(), sc.getVenue())
                 || !Objects.equals(ex.getCompetition(), sc.getCompetition())) {
                    ex.setVenue(sc.getVenue());
                    ex.setCompetition(sc.getCompetition());
                    toSave.add(ex);
                }
                keyToExisting.remove(key);
            } else {
                toSave.add(sc);
            }
        }

        for (UpcomingMatch stale : keyToExisting.values()) {
            if (stale.getKickoff().isBefore(now)) {
                stale.setStatus(MatchStatus.PLAYED);
                toSave.add(stale);
            } else {
                repo.delete(stale);
            }
        }

        return repo.saveAll(toSave);
    }

    public List<UpcomingMatch> scrapeUpcomingMatches(String url) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--headless=new", "--no-sandbox", "--disable-gpu", "--window-size=1920,1080");
        WebDriver driver = new ChromeDriver(opts);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        List<UpcomingMatch> matches = new ArrayList<>();

        try {
            driver.get(url);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("team-fixtures-body")));

            List<WebElement> rows = driver.findElements(By.cssSelector("#team-fixtures-body tr"));

            for (WebElement row : rows) {
                try {
                    String utcMillis = row.findElement(By.cssSelector("td.date")).getAttribute("data-utc");
                    long ms = Long.parseLong(utcMillis);
                    LocalDateTime kickoff = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.of("UTC"));

                    String home = row.findElement(By.cssSelector("td.home a.team-link")).getText().trim();
                    String away = row.findElement(By.cssSelector("td.away a.team-link")).getText().trim();
                    String competition = row.findElement(By.cssSelector("td.competition")).getText().trim();
                    String venue = row.findElement(By.cssSelector("td.venue")).getText().trim();

                    UpcomingMatch m = new UpcomingMatch();
                    m.setHomeTeam(home);
                    m.setAwayTeam(away);
                    m.setKickoff(kickoff);
                    m.setCompetition(competition);
                    m.setVenue(venue);

                    matches.add(m);
                } catch (NoSuchElementException | NumberFormatException e) {
                    continue;
                }
            }
        } finally {
            driver.quit();
        }

        return matches;
    }
 * 
 * 
 */

}


