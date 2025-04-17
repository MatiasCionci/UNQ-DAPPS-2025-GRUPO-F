package com.dappstp.dappstp.service.Scraping;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;
import com.dappstp.dappstp.model.Player;
import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.repository.PlayerBarcelonaRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScraperServicePlayers {

    private final PlayerBarcelonaRepository playerRepository;

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public List<PlayerBarcelona> scrapeAndSavePlayers() {
        List<PlayerBarcelona> players = scrapePlayers();
        // Guardar todos en la base de datos
        playerRepository.saveAll(players);
        return players;
    }

    public List<PlayerBarcelona> scrapePlayers() {
        // Ajustá la ruta de chromedriver en tu sistema
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\cionc\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        WebDriver driver = new ChromeDriver();

        List<PlayerBarcelona> players = new ArrayList<>();
        try {
            driver.get("https://es.whoscored.com/Teams/Barcelona");
            WebElement body = driver.findElement(By.id("player-table-statistics-body"));
            List<WebElement> rows = body.findElements(By.tagName("tr"));

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() >= 15) {
                    // Nombre (segunda línea dentro del primer td)
                    String raw = cols.get(0).getText();
                    String[] lines = raw.split("\\n");
                    String name = lines[1].trim();

                    String matches = cols.get(4).getText().trim();
                    int goals = parseIntSafe(cols.get(6).getText());
                    int assists = parseIntSafe(cols.get(7).getText());
                    double rating = parseDoubleSafe(cols.get(14).getText());

                    PlayerBarcelona player = new PlayerBarcelona();
                    player.setName(name);
                    player.setMatches(matches);
                    player.setGoals(goals);
                    player.setAssists(assists);
                    player.setRating(rating);

                    players.add(player);
                }
            }
        } finally {
            driver.quit();
        }
        return players;
    }

    private int parseIntSafe(String text) {
        try {
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String text) {
        try {
            return Double.parseDouble(text.trim().replace("-", "0"));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
