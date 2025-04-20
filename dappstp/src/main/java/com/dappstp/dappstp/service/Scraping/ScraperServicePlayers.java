package com.tuapp.service;

import com.tuapp.model.Player;
import com.tuapp.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperServicePlayers {

    private final PlayerRepository playerRepository;

    public void scrapePlayers() {
        RemoteWebDriver driver = null;

        try {
            ChromeOptions options = new ChromeOptions();
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
            options.addArguments("--no-sandbox");
            options.addArguments("--headless");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");

            driver = new RemoteWebDriver(new URL("http://selenium:4444/wd/hub"), options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            driver.get("https://es.whoscored.com/Teams/65/Show/Spain-Barcelona");

            // Cerrar pop-up si aparece
            try {
                WebElement popUp = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".qc-cmp2-summary-buttons .css-47sehv")));
                popUp.click();
            } catch (TimeoutException e) {
                log.info("No apareció el pop-up de cookies.");
            }

            // Seleccionar torneo: La Liga
            WebElement selectTournament = wait.until(ExpectedConditions.elementToBeClickable(By.id("stage")));
            selectTournament.click();

            List<WebElement> options = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#stage option")));
            for (WebElement option : options) {
                if (option.getText().contains("LaLiga")) {
                    option.click();
                    break;
                }
            }

            // Esperar que se actualice la tabla de jugadores
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("player-table-statistics-body")));

            List<Player> players = new ArrayList<>();
            WebElement tableBody = driver.findElement(By.id("player-table-statistics-body"));
            List<WebElement> rows = tableBody.findElements(By.tagName("tr"));

            for (WebElement row : rows) {
                try {
                    List<WebElement> cells = row.findElements(By.tagName("td"));

                    if (cells.size() < 10) continue;

                    String name = cells.get(0).getText().trim();
                    String position = cells.get(1).getText().trim();
                    int appearances = parseIntSafe(cells.get(2).getText().trim());
                    int goals = parseIntSafe(cells.get(3).getText().trim());
                    int assists = parseIntSafe(cells.get(4).getText().trim());
                    double yellowCards = parseDoubleSafe(cells.get(5).getText().trim());
                    double redCards = parseDoubleSafe(cells.get(6).getText().trim());
                    double rating = parseDoubleSafe(cells.get(7).getText().trim());

                    Player player = new Player(name, position, appearances, goals, assists, yellowCards, redCards, rating);
                    players.add(player);

                } catch (Exception e) {
                    log.warn("Error procesando fila de jugador: {}", e.getMessage());
                }
            }

            playerRepository.saveAll(players);
            log.info("Jugadores guardados correctamente: {}", players.size());

        } catch (Exception e) {
            log.error("Error durante scraping: {}", e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private int parseIntSafe(String txt) {
        try {
            return Integer.parseInt(txt.replaceAll("[^\\d]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0.0;
        try {
            // Reemplazar coma por punto y eliminar todo lo que no sea dígito o punto
            String cleaned = txt.replace(",", ".").replaceAll("[^\\d.]", "");

            if (cleaned.isEmpty() || cleaned.equals(".")) return 0.0;

            // Si hay más de un punto, quedarnos solo con el primero y los decimales inmediatos
            int firstDot = cleaned.indexOf('.');
            if (firstDot != -1) {
                int secondDot = cleaned.indexOf('.', firstDot + 1);
                if (secondDot != -1) {
                    cleaned = cleaned.substring(0, secondDot); // hasta el segundo punto
                }
            }

            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Error parseando double desde '{}': {}", txt, e.getMessage());
            return 0.0;
        } catch (Exception e) {
            log.warn("Error inesperado parseando double desde '{}': {}", txt, e.getMessage());
            return 0.0;
        }
    }
}
