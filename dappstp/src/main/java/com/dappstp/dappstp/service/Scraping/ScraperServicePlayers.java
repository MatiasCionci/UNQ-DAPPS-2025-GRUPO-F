package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.repository.PlayerBarcelonaRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.PageLoadStrategy;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ScraperServicePlayers {

    private final PlayerBarcelonaRepository playerRepository;

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() {
        log.info("🚀 Iniciando scraping de jugadores del Barcelona (v4 - con visibilidad)..."); // Log versionado
        List<PlayerBarcelona> players = new ArrayList<>();
        WebDriver driver = null;
        WebDriverWait wait = null; // Declarar fuera para usar en catch/finally
        String baseScreenshotPath = "/app/screenshot"; // Ruta base para screenshots en Render

        ChromeOptions options = new ChromeOptions();
        // options.setBinary("/usr/bin/google-chrome-stable"); // <-- Eliminado
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL); // Mantenido NORMAL, EAGER es otra opción
        options.addArguments(
            "--headless=new", // <-- Modo headless moderno
            "--no-sandbox",
            "--disable-setuid-sandbox", // Mantener si ayuda en Render
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.7049.95 Safari/537.36",
            "--user-data-dir=/tmp/chrome-profile-" + UUID.randomUUID()
            // Considerar añadir más opciones anti-detección si se sospecha de bloqueo
            // "--disable-blink-features=AutomationControlled"
        );

        try {
            log.info("Inicializando ChromeDriver...");
            driver = new ChromeDriver(options);
            wait = new WebDriverWait(driver, Duration.ofSeconds(120)); // Timeout largo
            log.info("WebDriver inicializado. Navegando a la página...");
            driver.get("https://www.whoscored.com/teams/65/show/spain-barcelona");
            log.info("Página cargada (según PageLoadStrategy NORMAL).");

            // SweetAlert (con wait corto)
            try {
                log.debug("Buscando SweetAlert...");
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
                By swalClose = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
                WebElement btn = shortWait.until(ExpectedConditions.visibilityOfElementLocated(swalClose));
                log.debug("SweetAlert encontrado, intentando cerrar...");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(swalClose)); // Esperar a que desaparezca (con wait largo)
                log.info("SweetAlert cerrado.");
            } catch (Exception e) {
                log.debug("SweetAlert no encontrado o ya cerrado (Timeout corto o error: {}).", e.getMessage());
            }

            // Cookies (con wait corto)
            try {
                log.debug("Buscando banner de cookies...");
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(15));
                shortWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                    By.cssSelector("iframe[title='SP Consent Message']")));
                log.debug("Dentro del iframe de cookies.");
                By acceptBtn = By.xpath("//button[contains(., 'Accept') or contains(., 'Aceptar')]");
                WebElement btn = shortWait.until(ExpectedConditions.elementToBeClickable(acceptBtn));
                log.debug("Botón Aceptar encontrado, intentando clic...");
                try {
                    btn.click();
                } catch (ElementClickInterceptedException ex) {
                    log.warn("Clic de cookies interceptado, usando JS.");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                }
                log.info("Cookies aceptadas.");
                // Espera corta después de aceptar cookies
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            } catch (Exception e) {
                log.debug("Banner de cookies no encontrado o error (Timeout corto o error: {}).", e.getMessage());
            } finally {
                log.debug("Volviendo al contenido principal.");
                driver.switchTo().defaultContent();
            }

            // Tabla inicial (solo para obtener referencia 'oldTable')
            log.debug("Esperando presencia de tabla inicial...");
            WebElement oldTable = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("player-table-statistics-body")));
            log.debug("Tabla inicial encontrada.");

            // Selección de LaLiga
            log.info("Intentando seleccionar LaLiga...");
            try {
                By torneoLocator = By.cssSelector("select[data-backbone-model-attribute-dd='tournamentOptions']");
                log.debug("Esperando que el selector de torneo sea clickeable...");
                WebElement selectElem = wait.until(ExpectedConditions.elementToBeClickable(torneoLocator));
                log.debug("Selector clickeable. Seleccionando 'LaLiga'...");
                new Select(selectElem).selectByVisibleText("LaLiga");
                log.info("Opción 'LaLiga' seleccionada.");

                log.debug("Esperando que la tabla antigua desaparezca (staleness)...");
                wait.until(ExpectedConditions.stalenessOf(oldTable));
                log.debug("Tabla antigua 'stale'. Esperando que la NUEVA tabla sea VISIBLE...");

                // --- CAMBIO CRÍTICO: ESPERAR VISIBILIDAD ---
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("player-table-statistics-body")));
                log.info("Nueva tabla de jugadores VISIBLE.");

            } catch (Exception e) {
                log.error("¡ERROR CRÍTICO! Fallo durante la selección de LaLiga o esperando la nueva tabla VISIBLE: {}", e.getMessage(), e);
                takeScreenshot(driver, baseScreenshotPath + "_laliga_table_visibility_error.png"); // Screenshot específico
                throw new RuntimeException("Fallo crítico al procesar selección de LaLiga y visibilidad de tabla.", e);
            }

            // Extracción
            log.info("Procediendo a extraer jugadores de la tabla...");

            // --- AÑADIR ESPERA PARA LAS FILAS ---
            log.debug("Esperando que aparezca al menos una fila en la tabla...");
            try {
                WebDriverWait rowWait = new WebDriverWait(driver, Duration.ofSeconds(15)); // Espera más corta para filas
                rowWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("#player-table-statistics-body tr"), 0));
                log.debug("Al menos una fila detectada.");
            } catch (TimeoutException ex) {
                log.warn("Timeout esperando filas (<tr>) dentro de la tabla visible. La tabla podría estar vacía o tardando demasiado en poblarse.");
                takeScreenshot(driver, baseScreenshotPath + "_no_rows_in_table.png");
                // Continuar, resultará en 0 jugadores si no hay filas
            }

            List<WebElement> rows = driver.findElements(By.cssSelector("#player-table-statistics-body tr"));
            log.info("Filas encontradas para procesar: {}", rows.size());

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() < 15) {
                     log.trace("Fila omitida, columnas insuficientes: {}", cols.size());
                     continue;
                }

                String name;
                try {
                    // Intenta obtener el nombre del span dentro del enlace (más específico)
                    WebElement nameSpan = cols.get(0).findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
                    name = nameSpan.getText().trim();
                    if (name.isEmpty()) { // Fallback si el span está vacío
                        name = cols.get(0).findElement(By.cssSelector("a.player-link")).getText().trim();
                        log.trace("Nombre obtenido por fallback de link (span vacío): {}", name);
                    } else {
                         log.trace("Nombre obtenido del span: {}", name);
                    }
                } catch (NoSuchElementException ex) {
                    // Fallback si no hay enlace/span: usar split en el texto de la celda
                    String cellText = cols.get(0).getText();
                    String[] parts = cellText.split("\\n");
                    name = parts.length > 1 ? parts[1].trim() : parts[0].trim(); // Toma la segunda línea si existe
                    log.trace("Nombre obtenido por fallback de texto directo (split): '{}' de '{}'", name, cellText);
                }

                if (name.isBlank()) {
                    log.warn("Nombre vacío detectado, fila omitida. Contenido celda[0]: {}", cols.get(0).getText());
                    continue;
                }

                PlayerBarcelona p = new PlayerBarcelona();
                p.setName(name);
                p.setMatches(cols.get(4).getText().trim());
                p.setGoals(parseIntSafe(cols.get(6).getText()));
                p.setAssists(parseIntSafe(cols.get(7).getText()));
                p.setRating(parseDoubleSafe(cols.get(14).getText()));
                players.add(p);
                log.trace("Jugador procesado: {}", name);
            }

            if (!players.isEmpty()) {
                log.info("Guardando {} jugadores...", players.size());
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else {
                log.warn("⚠️ No se procesaron jugadores válidos de la tabla.");
            }

        } catch (TimeoutException e) {
            String waitDuration = (wait != null) ? wait.toString() : "N/A";
            log.error("Timeout general ({}) esperando un elemento: {}", waitDuration, e.getMessage(), e);
            takeScreenshot(driver, baseScreenshotPath + "_general_timeout_error.png");
        } catch (WebDriverException e) {
             log.error("Error de WebDriver: {}", e.getMessage(), e);
             takeScreenshot(driver, baseScreenshotPath + "_webdriver_error.png");
        } catch (RuntimeException e) {
             log.error("Scraping detenido debido a error previo: {}", e.getMessage());
             // Screenshot ya tomado en el catch original
        }
        catch (Exception e) {
            log.error("Error general inesperado en scraping: {}", e.getMessage(), e);
            takeScreenshot(driver, baseScreenshotPath + "_unexpected_error.png");
        } finally {
            if (driver != null) {
                log.info("Cerrando WebDriver...");
                try {
                    driver.quit();
                    log.info("WebDriver cerrado.");
                } catch (Exception e) {
                    log.error("Error al cerrar WebDriver: {}", e.getMessage());
                }
            }
        }

        log.info("🏁 Scraping finalizado. Total procesados: {} jugadores.", players.size());
        return players;
    }

    // --- Métodos auxiliares ---

    private int parseIntSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0;
        try {
            String d = txt.split("\\(")[0].replaceAll("[^\\d]", "");
            return d.isEmpty() ? 0 : Integer.parseInt(d);
        } catch (Exception e) {
            log.warn("parseIntSafe falló para '{}': {}", txt, e.getMessage());
            return 0;
        }
    }

    private double parseDoubleSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0.0;
        try {
            String cleaned = txt.replace(",", ".").replaceAll("[^\\d.]", "");
            if (cleaned.isEmpty() || cleaned.equals(".")) return 0.0;
            int firstDot = cleaned.indexOf('.');
            if (firstDot != -1) {
                int secondDot = cleaned.indexOf('.', firstDot + 1);
                if (secondDot != -1) cleaned = cleaned.substring(0, secondDot);
            }
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            log.warn("parseDoubleSafe falló para '{}': {}", txt, e.getMessage());
            return 0.0;
        }
    }

    private void takeScreenshot(WebDriver driver, String path) {
        if (driver instanceof TakesScreenshot) {
            try {
                byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                try (FileOutputStream fos = new FileOutputStream(path)) {
                    fos.write(bytes);
                    log.warn("📸 Captura guardada en: {}", path);
                }
            } catch (Exception e) {
                log.error("No se pudo guardar screenshot en '{}': {}", path, e.getMessage());
            }
        } else if (driver != null) {
             log.warn("El WebDriver actual ({}) no soporta screenshots.", driver.getClass().getName());
        } else {
             log.warn("No se puede tomar screenshot, WebDriver es null.");
        }
    }
}
