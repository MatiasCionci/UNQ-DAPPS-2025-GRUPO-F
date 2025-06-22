package com.dappstp.dappstp.service.scraping.championsLeagueFinal;

import com.dappstp.dappstp.aspect.scraping.annotation.EnableScrapingSession;
import com.dappstp.dappstp.aspect.scraping.context.ScrapingContext;
import com.dappstp.dappstp.aspect.scraping.context.ScrapingContextHolder;
import com.dappstp.dappstp.exception.ScrapingException;
import com.dappstp.dappstp.util.ScrapingUtils;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class SimpleScorePredictionScraperService {

    @EnableScrapingSession
    public String scrapeScorePrediction(String matchUrl) {
        log.info("🚀 Iniciando scraping de predicción de resultado para: {}", matchUrl);

        try {
            ScrapingContext context = ScrapingContextHolder.getContext();
            WebDriver driver = context.getDriver();
            WebDriverWait wait = context.getWait();

            driver.get(matchUrl);
            log.info("Página cargada: {}", matchUrl);

            ScrapingUtils.closePopupIfPresent(wait); // Usar el método genérico para cerrar popups

            WebElement predictionDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("preview-prediction")));

            String homeTeamName = predictionDiv.findElement(By.cssSelector("div.home .team-link span")).getText().trim();
            String homeScoreStr = predictionDiv.findElement(By.cssSelector("div.home span.predicted-score")).getText().trim();

            String awayTeamName = predictionDiv.findElement(By.cssSelector("div.away .team-link span")).getText().trim();
            String awayScoreStr = predictionDiv.findElement(By.cssSelector("div.away span.predicted-score")).getText().trim();

            // Validar que los scores sean números
            Integer.parseInt(homeScoreStr); 
            Integer.parseInt(awayScoreStr);

            String resultString = String.format("Predicción: %s %s - %s %s",
                                                homeTeamName, homeScoreStr, awayScoreStr, awayTeamName);

            log.info("✅ Predicción de resultado extraída: {}", resultString);
            return resultString;

        } catch (NoSuchElementException e) {
            log.warn("No se pudo encontrar el elemento de predicción en {}: {}", matchUrl, e.getMessage());
            throw new ScrapingException("No se encontró la sección de predicción del resultado en la página para " + matchUrl, e);
        } catch (NumberFormatException e) {
            log.warn("El marcador obtenido no es un número válido en {}: {}", matchUrl, e.getMessage());
            throw new ScrapingException("Se encontró la sección de predicción, pero el formato del marcador no es válido para " + matchUrl, e);
        } catch (TimeoutException e) {
            log.warn("Timeout esperando la sección de predicción en {}: {}", matchUrl, e.getMessage());
            throw new ScrapingException("No se pudo cargar la sección de predicción a tiempo para " + matchUrl, e);
        } catch (Exception e) {
            log.error("Error general durante el scraping de predicción para {}: {}", matchUrl, e.getMessage(), e);
            throw new ScrapingException("Error general al obtener la predicción del resultado para " + matchUrl, e);
        }
    }
}