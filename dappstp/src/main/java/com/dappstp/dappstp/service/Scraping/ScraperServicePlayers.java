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
        log.info("🚀 Iniciando scraping de jugadores del Barcelona (v5 - staleness removed)..."); // Log versionado
        List<PlayerBarcelona> players = new ArrayList<>();
        WebDriver driver = null;
        WebDriverWait wait = null; // Declarar fuera para usar en catch/finally
        String baseScreenshotPath = "/app/screenshot"; // Ruta base para screenshots en Render

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-setuid-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.7049.95 Safari/537.36", // Use a recent Chrome version if possible
            "--user-data-dir=/tmp/chrome-profile-" + UUID.randomUUID()
            // "--disable-blink-features=AutomationControlled" // Consider if needed
        );

        try {
            log.info("Inicializando ChromeDriver...");
            driver = new ChromeDriver(options);
            wait = new WebDriverWait(driver, Duration.ofSeconds(120)); // Timeout largo general
            log.info("WebDriver inicializado. Navegando a la página...");
            driver.get("https://www.whoscored.com/teams/65/show/spain-barcelona");
            log.info("Página cargada (según PageLoadStrategy NORMAL).");

            // --- Pop-up Handling (Keep as is) ---
            // SweetAlert
            try {
                log.debug("Buscando SweetAlert...");
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
                By swalClose = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
                WebElement btn = shortWait.until(ExpectedConditions.visibilityOfElementLocated(swalClose));
                log.debug("SweetAlert encontrado, intentando cerrar...");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                // Use the main wait to ensure it's gone, as it might block other interactions
                wait.until(ExpectedConditions.invisibilityOfElementLocated(swalClose));
                log.info("SweetAlert cerrado.");
            } catch (Exception e) {
                log.debug("SweetAlert no encontrado o ya cerrado (Timeout corto o error: {}).", e.getMessage());
            }

            // Cookies
            try {
                log.debug("Buscando banner de cookies...");
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(15));
                shortWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                    By.cssSelector("iframe[title='SP Consent Message']")));
                log.debug("Dentro del iframe de cookies.");
                // More robust XPath for Accept button (handles different languages/texts)
                By acceptBtn = By.xpath("//button[.//span[contains(., 'Accept')] or contains(., 'Accept') or .//span[contains(., 'Aceptar')] or contains(., 'Aceptar')]");
                WebElement btn = shortWait.until(ExpectedConditions.elementToBeClickable(acceptBtn));
                log.debug("Botón Aceptar encontrado, intentando clic...");
                try {
                    btn.click();
                } catch (ElementClickInterceptedException ex) {
                    log.warn("Clic de cookies interceptado, usando JS.");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                }
                log.info("Cookies aceptadas.");
                // Wait for iframe to disappear after clicking accept
                driver.switchTo().defaultContent(); // Switch back first
                shortWait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("iframe[title='SP Consent Message']")));
                log.debug("Iframe de cookies desaparecido.");
            } catch (Exception e) {
                log.debug("Banner de cookies no encontrado o error (Timeout corto o error: {}).", e.getMessage());
                // Ensure we are back in the default content even if cookie handling failed
                try {
                    driver.switchTo().defaultContent();
                } catch (NoSuchFrameException nfex) {
                    log.trace("Ya estábamos en defaultContent o iframe no existía.");
                }
            }
            // --- End Pop-up Handling ---


            // Selección de LaLiga
            log.info("Intentando seleccionar LaLiga...");
            try {
                By torneoLocator = By.cssSelector("select[data-backbone-model-attribute-dd='tournamentOptions']");
                log.debug("Esperando que el selector de torneo sea clickeable...");
                WebElement selectElem = wait.until(ExpectedConditions.elementToBeClickable(torneoLocator));
                log.debug("Selector clickeable. Seleccionando 'LaLiga'...");
                new Select(selectElem).selectByVisibleText("LaLiga");
                log.info("Opción 'LaLiga' seleccionada.");

                // --- REMOVED STALENESS CHECK ---
                // log.debug("Esperando que la tabla antigua desaparezca (staleness)...");
                // WebElement oldTable = driver.findElement(By.id("player-table-statistics-body")); // Find it just before the wait if needed, but not strictly necessary now
                // wait.until(ExpectedConditions.stalenessOf(oldTable)); // <-- REMOVED THIS LINE
                // log.debug("Tabla antigua 'stale'.");

                // --- WAIT FOR VISIBILITY (Kept) ---
                // This wait ensures the table container is present/visible after the selection action.
                // It might be the same element, but its content should be updating.
                log.debug("Esperando que la tabla de jugadores sea VISIBLE después de seleccionar LaLiga...");
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("player-table-statistics-body")));
                log.info("Tabla de jugadores VISIBLE después de seleccionar LaLiga.");

                // --- ADDED: Short pause or more specific wait ---
                // Sometimes, even if visible, the content takes a fraction longer to render.
                // Option 1: Short explicit pause (less ideal but simple)
                 try { Thread.sleep(1000); } catch (InterruptedException ignored) {} // Pause 1 second
                 log.debug("Pequeña pausa después de visibilidad.");

                // Option 2: Wait for rows again (more robust) - Already done below, so maybe the pause is enough.

            } catch (Exception e) {
                log.error("¡ERROR CRÍTICO! Fallo durante la selección de LaLiga o esperando la tabla VISIBLE: {}", e.getMessage(), e);
                takeScreenshot(driver, baseScreenshotPath + "_laliga_select_or_visibility_error.png"); // Screenshot específico
                throw new RuntimeException("Fallo crítico al procesar selección de LaLiga y visibilidad de tabla.", e);
            }

            // Extracción
            log.info("Procediendo a extraer jugadores de la tabla...");

            // --- AÑADIR ESPERA PARA LAS FILAS (Kept) ---
            log.debug("Esperando que aparezca al menos una fila en la tabla...");
            WebElement tableBody = null; // Define here to use in catch block
            try {
                WebDriverWait rowWait = new WebDriverWait(driver, Duration.ofSeconds(30)); // Increased wait for rows slightly
                // Ensure we are waiting for rows *within* the specific table body
                tableBody = driver.findElement(By.id("player-table-statistics-body"));
                // Wait for at least one 'tr' element to be present *inside* the tableBody
                rowWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.xpath(".//tr"), 0));
                log.debug("Al menos una fila detectada dentro de la tabla.");
            } catch (TimeoutException ex) {
                log.warn("Timeout esperando filas (<tr>) dentro de la tabla visible. La tabla podría estar vacía o tardando demasiado en poblarse.");
                takeScreenshot(driver, baseScreenshotPath + "_no_rows_in_table.png");
                // Log content of table body if possible
                if (tableBody != null) {
                    log.warn("Contenido HTML del tbody en el momento del timeout: {}", tableBody.getAttribute("innerHTML"));
                }
                // Continuar, resultará en 0 jugadores si no hay filas
            } catch (NoSuchElementException nse) {
                 log.error("Error crítico: No se encontró el elemento #player-table-statistics-body para buscar filas.", nse);
                 takeScreenshot(driver, baseScreenshotPath + "_tbody_not_found_for_rows.png");
                 throw new RuntimeException("No se pudo encontrar el tbody para extraer filas.", nse);
            }

            // Re-find rows after waiting to avoid stale elements if tableBody was found earlier
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
                    // Prioritize the specific span within the link
                    WebElement nameSpan = cols.get(0).findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
                    name = nameSpan.getText().trim();
                    if (name.isBlank()) { // Check if span exists but is empty
                        name = cols.get(0).findElement(By.cssSelector("a.player-link")).getText().trim();
                        log.trace("Nombre obtenido por fallback de link (span vacío o en blanco): {}", name);
                    } else {
                         log.trace("Nombre obtenido del span: {}", name);
                    }
                } catch (NoSuchElementException ex) {
                    // Fallback if no link/span: use split on the cell text
                    String cellText = cols.get(0).getText();
                    String[] parts = cellText.split("\\n"); // Split by newline
                    // Usually name is on the second line if multiple lines exist
                    name = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                    log.trace("Nombre obtenido por fallback de texto directo (split): '{}' de '{}'", name, cellText);
                }

                // Final check for blank name after all attempts
                if (name.isBlank()) {
                    log.warn("Nombre vacío detectado después de todos los fallbacks, fila omitida. Contenido celda[0]: {}", cols.get(0).getText());
                    continue;
                }

                PlayerBarcelona p = new PlayerBarcelona();
                p.setName(name);
                // Extract other data (ensure indices are correct)
                p.setMatches(cols.get(4).getText().trim()); // Column 5 (index 4)
                p.setGoals(parseIntSafe(cols.get(6).getText()));   // Column 7 (index 6)
                p.setAssists(parseIntSafe(cols.get(7).getText())); // Column 8 (index 7)
                p.setRating(parseDoubleSafe(cols.get(14).getText()));// Column 15 (index 14)
                players.add(p);
                log.trace("Jugador procesado: {}", p.getName()); // Log the processed player name
            }

            if (!players.isEmpty()) {
                log.info("Guardando {} jugadores...", players.size());
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else {
                log.warn("⚠️ No se procesaron jugadores válidos de la tabla. Verifique los logs de traza y las capturas de pantalla si se generaron.");
                takeScreenshot(driver, baseScreenshotPath + "_no_valid_players_processed.png");
            }

        } catch (TimeoutException e) {
            String waitDuration = (wait != null) ? wait.toString() : "N/A";
            log.error("Timeout general ({}) esperando un elemento: {}", waitDuration, e.getMessage(), e);
            takeScreenshot(driver, baseScreenshotPath + "_general_timeout_error.png");
        } catch (WebDriverException e) {
             log.error("Error de WebDriver: {}", e.getMessage(), e);
             // Check if it's a session closed error, which might happen during long waits
             if (e.getMessage() != null && e.getMessage().contains("session deleted or not found")) {
                 log.error("La sesión del WebDriver parece haberse cerrado inesperadamente.");
             }
             takeScreenshot(driver, baseScreenshotPath + "_webdriver_error.png");
        } catch (RuntimeException e) {
             // Catch the re-thrown exception from the LaLiga selection block
             log.error("Scraping detenido debido a error previo: {}", e.getMessage());
             // Screenshot should have been taken in the original catch block
        } catch (Exception e) {
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

    // --- Métodos auxiliares (Keep as is) ---

    private int parseIntSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0;
        try {
            // Handle values like "1(2)" -> take only "1"
            String numPart = txt.split("\\(")[0].trim();
            // Remove any non-digit characters just in case (though usually not needed for goals/assists)
            String digits = numPart.replaceAll("[^\\d]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            log.warn("parseIntSafe falló para '{}': {}", txt, e.getMessage());
            return 0;
        }
    }

    private double parseDoubleSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0.0;
        try {
            // Replace comma with dot for decimal separator consistency
            String cleaned = txt.replace(",", ".");
            // Remove any characters that are not digits or a dot
            cleaned = cleaned.replaceAll("[^\\d.]", "");
            // Handle cases like empty string or just "." after cleaning
            if (cleaned.isEmpty() || cleaned.equals(".")) return 0.0;
            // Ensure only one decimal point (take everything before the second dot if present)
            int firstDot = cleaned.indexOf('.');
            if (firstDot != -1) {
                int secondDot = cleaned.indexOf('.', firstDot + 1);
                if (secondDot != -1) {
                    cleaned = cleaned.substring(0, secondDot);
                }
            }
            // Handle potential leading/trailing dots after cleaning complex strings
             if (cleaned.startsWith(".")) cleaned = "0" + cleaned;
             if (cleaned.endsWith(".")) cleaned = cleaned.substring(0, cleaned.length() - 1);
             if (cleaned.isEmpty()) return 0.0; // Check again after potential trimming

            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
             log.warn("parseDoubleSafe falló para '{}' (NumberFormatException): {}", txt, e.getMessage());
             return 0.0;
        } catch (Exception e) {
            log.warn("parseDoubleSafe falló para '{}' (Exception): {}", txt, e.getMessage());
            return 0.0;
        }
    }


    private void takeScreenshot(WebDriver driver, String path) {
        if (driver instanceof TakesScreenshot) {
            try {
                // Ensure the directory exists (useful in container environments)
                // File screenshotFile = new File(path);
                // File parentDir = screenshotFile.getParentFile();
                // if (parentDir != null && !parentDir.exists()) {
                //     log.info("Creando directorio para screenshots: {}", parentDir.getAbsolutePath());
                //     parentDir.mkdirs();
                // }

                byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                try (FileOutputStream fos = new FileOutputStream(path)) {
                    fos.write(bytes);
                    log.warn("📸 Captura guardada en: {}", path);
                }
            } catch (WebDriverException wde) {
                 log.error("Error de WebDriver al tomar screenshot para '{}': {}", path, wde.getMessage());
            }
            catch (Exception e) {
                log.error("No se pudo guardar screenshot en '{}': {}", path, e.getMessage());
            }
        } else if (driver != null) {
             log.warn("El WebDriver actual ({}) no soporta screenshots.", driver.getClass().getName());
        } else {
             log.warn("No se puede tomar screenshot, WebDriver es null.");
        }
    }
}
