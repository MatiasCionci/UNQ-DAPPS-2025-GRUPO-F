package com.dappstp.dappstp.service.scraping.championsLeagueFinal;


import com.dappstp.dappstp.model.scraping.TeamStatsSummaryEntity;
import com.dappstp.dappstp.model.scraping.StatDetailEntity;
import com.dappstp.dappstp.repository.TeamStatsSummaryRepository;
import com.dappstp.dappstp.aspect.scraping.annotation.EnableScrapingSession;
import com.dappstp.dappstp.aspect.scraping.context.ScrapingContext;
import com.dappstp.dappstp.aspect.scraping.context.ScrapingContextHolder;
import com.dappstp.dappstp.dto.championsLeague.StatDetailDto;
import com.dappstp.dappstp.dto.championsLeague.TeamStatsSummaryDto;
import com.dappstp.dappstp.exception.ScrapingException;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CLFinalTeamStatsSummaryScraperService {

    private final TeamStatsSummaryRepository statsSummaryRepository;

    public CLFinalTeamStatsSummaryScraperService(TeamStatsSummaryRepository statsSummaryRepository) {
        this.statsSummaryRepository = statsSummaryRepository;
    }

    @EnableScrapingSession
    public TeamStatsSummaryDto scrapeTeamStatsSummary(String matchUrl) {
        log.info("🚀 Iniciando scraping de resumen de estadísticas de equipo para: {}", matchUrl);
        TeamStatsSummaryDto summaryDto = new TeamStatsSummaryDto();
        List<StatDetailDto> statDetailsDtoList = new ArrayList<>();
        TeamStatsSummaryEntity summaryEntity = new TeamStatsSummaryEntity();
        // summaryEntity.setMatchUrl(matchUrl); // Opcional: si quieres guardar la URL

        try {
            ScrapingContext context = ScrapingContextHolder.getContext();
            WebDriver driver = context.getDriver();
            WebDriverWait wait = context.getWait();

            driver.get(matchUrl);
            log.info("Página cargada: {}", matchUrl);

            closePopupIfPresent(wait, driver);

            WebElement statsSummaryContent = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.id("ws-team-stats-summary-content")
            ));
            log.debug("Contenedor principal de estadísticas encontrado.");

            // Extraer información del equipo local (izquierda)
            WebElement leftTeamDiv = statsSummaryContent.findElement(By.cssSelector("div.left"));
            String homeEmblemUrl = leftTeamDiv.findElement(By.cssSelector("img.team-emblem")).getAttribute("src");
            String homePlayedText = leftTeamDiv.findElement(By.cssSelector("li.played")).getText().replace("Jugados:", "").trim();
            summaryDto.setHomeTeamEmblemUrl(homeEmblemUrl);
            summaryDto.setHomeMatchesPlayed(homePlayedText);
            summaryEntity.setHomeTeamEmblemUrl(homeEmblemUrl);
            summaryEntity.setHomeMatchesPlayed(homePlayedText);

            // Extraer información del equipo visitante (derecha)
            WebElement rightTeamDiv = statsSummaryContent.findElement(By.cssSelector("div.right"));
            String awayEmblemUrl = rightTeamDiv.findElement(By.cssSelector("img.team-emblem")).getAttribute("src");
            String awayPlayedText = rightTeamDiv.findElement(By.cssSelector("li.played")).getText().replace("Jugados:", "").trim();
            summaryDto.setAwayTeamEmblemUrl(awayEmblemUrl);
            summaryDto.setAwayMatchesPlayed(awayPlayedText);
            summaryEntity.setAwayTeamEmblemUrl(awayEmblemUrl);
            summaryEntity.setAwayMatchesPlayed(awayPlayedText);

            // Extraer estadísticas comparativas del centro
            WebElement centreStatsDiv = statsSummaryContent.findElement(By.cssSelector("div.centre"));
            List<WebElement> statElements = centreStatsDiv.findElements(By.cssSelector("div.stat"));
            log.info("Número de filas de estadísticas encontradas: {}", statElements.size());

            for (WebElement statElement : statElements) {
                List<WebElement> statValues = statElement.findElements(By.cssSelector("span.stat-value span.pulsable"));
                String homeValue = statValues.get(0).getText().trim();
                String label = statElement.findElement(By.cssSelector("span.stat-label")).getText().trim();
                String awayValue = statValues.get(1).getText().trim();

                statDetailsDtoList.add(new StatDetailDto(label, homeValue, awayValue));

                StatDetailEntity statEntity = new StatDetailEntity(label, homeValue, awayValue);
                summaryEntity.addStatDetail(statEntity); // Esto también establece la relación bidireccional

                log.debug("Estadística extraída: {} - Local: {}, Visitante: {}", label, homeValue, awayValue);
            }
            summaryDto.setStats(statDetailsDtoList);

            statsSummaryRepository.save(summaryEntity);
            log.info("✅ Resumen de estadísticas de equipo guardado y extraído exitosamente para {}. ID Entidad: {}", matchUrl, summaryEntity.getId());
            return summaryDto;

        } catch (NoSuchElementException e) {
            log.warn("No se pudo encontrar un elemento esperado en {}: {}", matchUrl, e.getMessage());
            throw new ScrapingException("No se encontró un elemento HTML necesario para el scraping de estadísticas en " + matchUrl, e);
        } catch (TimeoutException e) {
            log.warn("Timeout esperando un elemento en {}: {}", matchUrl, e.getMessage());
            throw new ScrapingException("No se pudo cargar un elemento a tiempo para el scraping de estadísticas en " + matchUrl, e);
        } catch (Exception e) {
            log.error("Error general durante el scraping de estadísticas para {}: {}", matchUrl, e.getMessage(), e);
            throw new ScrapingException("Error general al obtener el resumen de estadísticas para " + matchUrl, e);
        }
    }

    private void closePopupIfPresent(WebDriverWait wait, WebDriver driver) {
        try {
            WebElement webpushModal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.webpush-swal2-shown")));
            webpushModal.findElement(By.cssSelector("button.webpush-swal2-close")).click();
            wait.until(ExpectedConditions.invisibilityOf(webpushModal));
            log.debug("🎉 Popup de webpush cerrado.");
        } catch (TimeoutException | NoSuchElementException e) {
            log.debug("Popup de webpush no apareció o no se pudo cerrar.");
        }

        try {
            WebElement cookieConsentModal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("qc-cmp2-container")));
            List<WebElement> consentButtons = cookieConsentModal.findElements(By.cssSelector("button[mode='primary'], button.qc-cmp2-button[mode='primary']"));
            if (!consentButtons.isEmpty()) {
                consentButtons.get(0).click();
                wait.until(ExpectedConditions.invisibilityOf(cookieConsentModal));
                log.debug("🎉 Popup de cookie consent cerrado.");
            }
        } catch (TimeoutException | NoSuchElementException ex) {
            log.debug("Popup de cookie consent no apareció o no se pudo cerrar.");
        }
    }
     @Transactional(readOnly = true)
     public TeamStatsSummaryDto retrieveLatestTeamStatsSummaryFromDB() {
        log.info("Intentando recuperar el último TeamStatsSummary desde la BD...");
        Optional<TeamStatsSummaryEntity> entityOptional = statsSummaryRepository.findTopByOrderByIdDesc(); 

        if (entityOptional.isPresent()) {
            TeamStatsSummaryEntity entity = entityOptional.get();
            log.info("Último TeamStatsSummaryEntity encontrado en la BD con ID: {}", entity.getId());
            return convertEntityToDto(entity);
        } else {
            log.warn("No se encontró ningún TeamStatsSummaryEntity en la BD.");
            return null; 
        }
    }

    // Método ayudante para convertir la Entidad a DTO
    private TeamStatsSummaryDto convertEntityToDto(TeamStatsSummaryEntity entity) {
        if (entity == null) {
            return null;
        }
        TeamStatsSummaryDto dto = new TeamStatsSummaryDto();
        dto.setHomeTeamEmblemUrl(entity.getHomeTeamEmblemUrl());
        dto.setHomeMatchesPlayed(entity.getHomeMatchesPlayed());
        dto.setAwayTeamEmblemUrl(entity.getAwayTeamEmblemUrl());
        dto.setAwayMatchesPlayed(entity.getAwayMatchesPlayed());
        if (entity.getStats() != null) { // Usamos getStats() que es generado por Lombok
            List<StatDetailDto> statDetailsDtoList = entity.getStats().stream()
                .map(statEntity -> new StatDetailDto(statEntity.getLabel(), statEntity.getHomeValue(), statEntity.getAwayValue()))
                .collect(Collectors.toList());
            dto.setStats(statDetailsDtoList);
        } else {
            dto.setStats(new ArrayList<>());
        }
        return dto;
    }
}
    // Método ayudante para convertir la Entidad a DTO
  
