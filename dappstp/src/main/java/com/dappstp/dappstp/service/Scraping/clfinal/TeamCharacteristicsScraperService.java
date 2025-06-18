package com.dappstp.dappstp.service.scraping.clfinal;
import com.dappstp.dappstp.service.scraping.aspect.annotation.EnableScrapingSession;
import com.dappstp.dappstp.service.scraping.aspect.context.ScrapingContext;
import com.dappstp.dappstp.service.scraping.aspect.context.ScrapingContextHolder;
import com.dappstp.dappstp.exception.ScrapingException;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TeamCharacteristicsScraperService {

    @EnableScrapingSession
    public List<String> scrapeTeamCharacteristics(String matchUrl) {
        log.info("🚀 Iniciando scraping de características de equipo para: {}", matchUrl);
        List<String> characteristics = new ArrayList<>();

        try {
            ScrapingContext context = ScrapingContextHolder.getContext();
            WebDriver driver = context.getDriver();
            WebDriverWait wait = context.getWait();

            driver.get(matchUrl);
            log.info("Página cargada: {}", matchUrl);

            closePopupIfPresent(wait); // Reutilizamos el método para cerrar popups

            // Esperar a que la tabla de características sea visible
            WebElement characteristicsTable = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("table.grid.teamcharacter")
            ));
            log.debug("Tabla de características encontrada.");

            // Obtener todas las filas del tbody
            List<WebElement> rows = characteristicsTable.findElements(By.cssSelector("tbody tr"));
            log.info("Número de características encontradas: {}", rows.size());

            for (WebElement row : rows) {
                // El texto de la característica está en el primer td
                WebElement characteristicCell = row.findElement(By.cssSelector("td:first-child"));
                String characteristicText = characteristicCell.getText().trim();
                characteristics.add(characteristicText);
                log.debug("Característica extraída: {}", characteristicText);
            }

            log.info("✅ {} características de equipo extraídas exitosamente.", characteristics.size());
            return characteristics;

        } catch (NoSuchElementException e) {
            log.warn("No se pudo encontrar la tabla de características en {}: {}", matchUrl, e.getMessage());
            throw new ScrapingException("No se encontró la tabla de características del equipo en la página para " + matchUrl, e);
        } catch (TimeoutException e) {
            log.warn("Timeout esperando la tabla de características en {}: {}", matchUrl, e.getMessage());
            throw new ScrapingException("No se pudo cargar la tabla de características a tiempo para " + matchUrl, e);
        } catch (Exception e) {
            log.error("Error general durante el scraping de características para {}: {}", matchUrl, e.getMessage(), e);
            throw new ScrapingException("Error general al obtener las características del equipo para " + matchUrl, e);
        }
    }

    // Este método es idéntico al de SimpleScorePredictionScraperService.
    // Podrías considerar moverlo a una clase de utilidad si lo usas en muchos servicios de scraping.
    private void closePopupIfPresent(WebDriverWait wait) {
        try {
            WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.webpush-swal2-shown, div#qc-cmp2-container")
            ));
            try { // Intenta con el botón de cerrar de webpush
                WebElement closeBtn = modal.findElement(By.cssSelector("button.webpush-swal2-close"));
                closeBtn.click(); wait.until(ExpectedConditions.invisibilityOf(modal)); log.debug("🎉 Popup de webpush cerrado."); return;
            } catch (NoSuchElementException | TimeoutException e) { log.debug("No se encontró el botón de cerrar de webpush, intentando con cookie consent."); }
            try { // Intenta con el botón de aceptar de cookie consent
                List<WebElement> consentButtons = modal.findElements(By.cssSelector("button[mode='primary'], button.qc-cmp2-button[mode='primary']"));
                if (!consentButtons.isEmpty()) { consentButtons.get(0).click(); wait.until(ExpectedConditions.invisibilityOf(modal)); log.debug("🎉 Popup de cookie consent cerrado."); }
                else { log.debug("No se encontró botón de aceptación de cookies conocido."); }
            } catch (NoSuchElementException | TimeoutException e) { log.debug("No se pudo cerrar el popup de cookie consent: {}", e.getMessage()); }
        } catch (TimeoutException | NoSuchElementException e) { log.debug("No apareció ningún popup conocido o no se pudo cerrar."); }
    }
}