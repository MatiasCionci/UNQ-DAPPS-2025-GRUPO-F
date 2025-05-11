
package com.dappstp.dappstp.service.Scraping.service;
import com.dappstp.dappstp.service.Scraping.aspect.annotation.EnableScrapingSession;
import com.dappstp.dappstp.service.Scraping.aspect.context.ScrapingContext;
import com.dappstp.dappstp.service.Scraping.aspect.context.ScrapingContextHolder;
import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.repository.PlayersRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ScraperServicePlayers {

    private final PlayersRepository playerRepository;

    public ScraperServicePlayers(PlayersRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    @EnableScrapingSession
    public List<Players> scrapeAndSavePlayers() {
        log.info("🚀 Iniciando lógica de scraping para jugadores (WebDriver gestionado por AOP)...");
        List<Players> players = new ArrayList<>();
        try {
            ScrapingContext context = ScrapingContextHolder.getContext();
            WebDriver driver = context.getDriver();
            WebDriverWait wait = context.getWait();

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
            log.error("Error durante el proceso de scraping de jugadores: {}", e.getMessage(), e);
            // La excepción será propagada y el @Around advice en WebDriverManagementAspect se encargará del driver.quit()
            throw new RuntimeException("Fallo en el scraping de jugadores", e); // O manejarla como prefieras para no cortar el flujo si es parte de un proceso mayor
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