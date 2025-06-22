package com.dappstp.dappstp.util;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@Slf4j
public final class ScrapingUtils {

    private ScrapingUtils() {
        // Clase de utilidad, no instanciable
    }

    public static void closePopupIfPresent(WebDriverWait wait) {
        try {
            WebElement modal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.webpush-swal2-shown, div#qc-cmp2-container")
            ));
            // Intenta con el botón de cerrar de webpush
            try {
                WebElement closeBtn = modal.findElement(By.cssSelector("button.webpush-swal2-close"));
                closeBtn.click();
                wait.until(ExpectedConditions.invisibilityOf(modal));
                log.debug("🎉 Popup de webpush cerrado.");
                return;
            } catch (NoSuchElementException | TimeoutException e) {
                log.debug("No se encontró el botón de cerrar de webpush, intentando con cookie consent.");
            }
            // Intenta con el botón de aceptar de cookie consent
            try {
                List<WebElement> consentButtons = modal.findElements(By.cssSelector("button[mode='primary'], button.qc-cmp2-button[mode='primary']"));
                if (!consentButtons.isEmpty()) {
                    consentButtons.get(0).click();
                    wait.until(ExpectedConditions.invisibilityOf(modal));
                    log.debug("🎉 Popup de cookie consent cerrado.");
                } else {
                     log.debug("No se encontró botón de aceptación de cookies conocido.");
                }
            } catch (NoSuchElementException | TimeoutException e) {
                log.debug("No se pudo cerrar el popup de cookie consent: {}", e.getMessage());
            }
        } catch (TimeoutException | NoSuchElementException e) {
            log.debug("No apareció ningún popup conocido o no se pudo cerrar.");
        }
    }
}