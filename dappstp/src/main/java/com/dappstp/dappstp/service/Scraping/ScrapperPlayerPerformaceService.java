package com.dappstp.dappstp.service.scraping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import com.dappstp.dappstp.model.PlayerPerformance;
import com.dappstp.dappstp.service.WhoScoredScraper;

import jakarta.annotation.PreDestroy;

@Service
public class ScrapperPlayerPerformaceService implements WhoScoredScraper {
    private WebDriver driver;

    private WebDriver getDriver() {
        if (driver == null) {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless", "--disable-gpu");
            // Eliminamos --user-data-dir para evitar colisiones
            driver = new ChromeDriver(options);
        }
        return driver;
    }

    @Override
    public PlayerPerformance scrapeRecentStats(String playerId) {
        WebDriver drv = getDriver();
        drv.get("https://www.whoscored.com/Players/" + playerId + "/Show/");

        Map<String, Double> metrics = new HashMap<>();
        WebElement goals   = drv.findElement(By.cssSelector(".player-goals .value"));
        WebElement assists = drv.findElement(By.cssSelector(".player-assists .value"));

        metrics.put("goals",   Double.parseDouble(goals.getText()));
        metrics.put("assists", Double.parseDouble(assists.getText()));

        return new PlayerPerformance(playerId, metrics);
    }

    @Override
    public List<PlayerPerformance> scrapeHistoricalStats(String playerId) {
        WebDriver drv = getDriver();
        drv.get("https://www.whoscored.com/Players/" + playerId + "/History/");

        List<PlayerPerformance> list = new ArrayList<>();
        for (WebElement row : drv.findElements(By.cssSelector("table.history-table tbody tr"))) {
            double g = Double.parseDouble(row.findElement(By.cssSelector("td.goals")).getText());
            double a = Double.parseDouble(row.findElement(By.cssSelector("td.assists")).getText());
            list.add(new PlayerPerformance(playerId, Map.of("goals", g, "assists", a)));
        }
        return list;
    }

    @PreDestroy
    public void cleanup() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
    }
}
