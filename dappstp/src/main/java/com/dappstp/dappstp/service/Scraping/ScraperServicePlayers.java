package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.Player;
import com.dappstp.dappstp.repository.PlayerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperServicePlayers {

    private final PlayerRepository playerRepository;

    @Value("${scraping.barcelona.url}")
    private String barcelonaUrl;

    @Transactional
    public void scrapeAndSavePlayers() {
        log.info("🚀 Iniciando scraping de jugadores del Barcelona...");

        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get(barcelonaUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("player-table-statistics-body")));
            } catch (NoSuchElementException e) {
                log.error("❌ Elemento con id 'player-table-statistics-body' no encontrado. Detalles: {}", e.getMessage());
                return;
            }

            WebElement tableBody = driver.findElement(By.id("player-table-statistics-body"));
            List<WebElement> rows = tableBody.findElements(By.tagName("tr"));

            List<Player> players = new ArrayList<>();
            for (WebElement row : rows) {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                if (cells.size() > 1) {
                    String name = cells.get(0).getText();
                    String position = cells.get(1).getText();
                    players.add(new Player(name, position));
                }
            }

            playerRepository.saveAll(players);
            log.info("🏁 Scraping finalizado. Total: {} jugadores.", players.size());

        } catch (Exception e) {
            log.error("Error general en scraping: {}", e.getMessage());
        } finally {
            driver.quit();
        }
    }
}
