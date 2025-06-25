package com.dappstp.dappstp.service.advancedmetrics;
import com.dappstp.dappstp.dto.metricasAvanzadas.TeamDataDto;
import com.dappstp.dappstp.exception.ScrapingException;
import com.dappstp.dappstp.util.ScrapingUtils;
import ch.qos.logback.classic.util.ContextInitializer;
import com.dappstp.dappstp.aspect.scraping.annotation.EnableScrapingSession;
import com.dappstp.dappstp.aspect.scraping.context.ScrapingContext;
import com.dappstp.dappstp.aspect.scraping.context.ScrapingContextHolder;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class TeamDataScraperService {
  

        private static final Logger logger = LoggerFactory.getLogger("AuditLogger");

@EnableScrapingSession
    public TeamDataDto scrapeDataSummary(String teamUrl) {
        log.info("🚀 Iniciando scraping de resumen de estadísticas de equipo para: {}", teamUrl);
        logger.info("🚀 Iniciando scraping de resumen de estadísticas de equipo para: {}", teamUrl) ;
        
        try {
            ScrapingContext context = ScrapingContextHolder.getContext();
            WebDriver driver = context.getDriver();
            WebDriverWait wait = context.getWait();

            driver.get(teamUrl);
            log.info("Página cargada: {}", teamUrl);

            ScrapingUtils.closePopupIfPresent(wait);

            // Esperar a que la tabla de resumen de estadísticas sea visible
            WebElement statsTable = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("top-team-stats-summary-grid")
            ));
            log.debug("Tabla de estadísticas de resumen encontrada.");

            // Localizar la fila de "Total / Promedio" de forma robusta usando XPath
            // Busca una fila (tr) que contenga un td con un strong cuyo texto sea exactamente 'Total / Promedio'
            WebElement totalRow = statsTable.findElement(By.xpath(".//tr[td/strong[text()='Total / Promedio']]"));
            log.debug("Fila de 'Total / Promedio' encontrada.");

            List<WebElement> cells = totalRow.findElements(By.tagName("td"));

            // Extraer los datos de cada celda
            int apps = parseIntSafely(cells.get(1).getText());
            int goals = parseIntSafely(cells.get(2).getText());
            double shotsPerGame = parseDoubleSafely(cells.get(3).getText());

            // La celda de disciplina contiene dos spans, uno para amarillas y otro para rojas
            WebElement disciplineCell = cells.get(4);
            int yellowCards = parseIntSafely(disciplineCell.findElement(By.cssSelector("span.yellow-card-box")).getText());
            int redCards = parseIntSafely(disciplineCell.findElement(By.cssSelector("span.red-card-box")).getText());

            double possession = parseDoubleSafely(cells.get(5).getText());
            double passSuccess = parseDoubleSafely(cells.get(6).getText());
            double aerialsWon = parseDoubleSafely(cells.get(7).getText());
            double rating = parseDoubleSafely(cells.get(8).getText());

            TeamDataDto statsDto = TeamDataDto.builder()
                .apps(apps)
                .goals(goals)
                .shotsPerGame(shotsPerGame)
                .yellowCards(yellowCards)
                .redCards(redCards)
                .possessionPercentage(possession)
                .passSuccessPercentage(passSuccess)
                .aerialsWonPerGame(aerialsWon)
                .rating(rating)
                .build();

            log.info("✅ Resumen de estadísticas de equipo extraído exitosamente: {}", statsDto);
            return statsDto;

        } catch (NoSuchElementException e) {
            log.warn("No se pudo encontrar un elemento requerido en la tabla de estadísticas en {}: {}", teamUrl, e.getMessage());
            throw new ScrapingException("No se encontró la tabla de estadísticas o la fila de totales en la página para " + teamUrl, e);
        } catch (TimeoutException e) {
            log.warn("Timeout esperando la tabla de estadísticas en {}: {}", teamUrl, e.getMessage());
            throw new ScrapingException("No se pudo cargar la tabla de estadísticas a tiempo para " + teamUrl, e);
        } catch (Exception e) {
            log.error("Error general durante el scraping de estadísticas de equipo para {}: {}", teamUrl, e.getMessage(), e);
            throw new ScrapingException("Error general al obtener las estadísticas del equipo para " + teamUrl, e);
        }
    }

    /**
     * Convierte un String a double de forma segura, manejando valores no numéricos.
     * @param text El texto a convertir.
     * @return El valor double o 0.0 si no se puede convertir.
     */
    private double parseDoubleSafely(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException | NullPointerException e) {
            return 0.0;
        }
    }

    /**
     * Convierte un String a int de forma segura, manejando valores no numéricos.
     * @param text El texto a convertir.
     * @return El valor int o 0 si no se puede convertir.
     */
    private int parseIntSafely(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }
}
