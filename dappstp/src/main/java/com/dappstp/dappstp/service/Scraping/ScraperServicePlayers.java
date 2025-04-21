package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.repository.PlayerBarcelonaRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
// import org.openqa.selenium.support.ui.Select; // No se usa
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.PageLoadStrategy;
import org.springframework.stereotype.Service;

// Importa File si descomentas la creación de directorio para screenshots
// import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
// Importa UUID si vuelves a usarlo para user-data-dir
// import java.util.UUID;

@Service
@Slf4j
public class ScraperServicePlayers { // INICIO CLASE

    private final PlayerBarcelonaRepository playerRepository;
    private final String baseScreenshotPath = "/app/screenshot";

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() { // INICIO MÉTODO scrapeAndSavePlayers
        // ACTUALIZACIÓN v12: Extracción de tabla robusta (scroll + manejo StaleElement en loop).
        //               ¡¡¡IMPORTANTE!!! RE-VERIFICA shm_size y RAM.
        log.info("🚀 Iniciando scraping de jugadores del Barcelona (v12 - robust table extraction)...");
        List<PlayerBarcelona> players = new ArrayList<>();
        WebDriver driver = null;
        WebDriverWait wait = null;

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--disable-extensions",
            "--window-size=1920,1080"
        );

        try {
            log.info("Inicializando ChromeDriver (v12)...");
            driver = new ChromeDriver(options);
            log.info("ChromeDriver inicializado correctamente.");
            wait = new WebDriverWait(driver, Duration.ofSeconds(150));
            log.info("Navegando a la página...");
            driver.get("https://www.whoscored.com/teams/65/show/spain-barcelona");
            log.info("Página cargada (según PageLoadStrategy NORMAL).");

            // --- Manejo de Pop-ups (Sin cambios v9) ---
            boolean popupHandled = false;
            // SweetAlert
            try {
                log.debug("Buscando SweetAlert...");
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
                By swalClose = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
                WebElement btn = shortWait.until(ExpectedConditions.visibilityOfElementLocated(swalClose));
                log.debug("SweetAlert encontrado, intentando cerrar...");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(swalClose));
                log.info("SweetAlert cerrado.");
                popupHandled = true;
            } catch (Exception e) { log.debug("SweetAlert no encontrado o ya cerrado."); }
            // Cookies
            try {
                log.debug("Buscando banner de cookies...");
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(15));
                shortWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.cssSelector("iframe[title='SP Consent Message']")));
                log.debug("Dentro del iframe de cookies.");
                By acceptBtn = By.xpath("//button[.//span[contains(., 'Accept')] or contains(., 'Accept') or .//span[contains(., 'Aceptar')] or contains(., 'Aceptar')]");
                WebElement btn = shortWait.until(ExpectedConditions.elementToBeClickable(acceptBtn));
                log.debug("Botón Aceptar encontrado, intentando clic...");
                try { btn.click(); } catch (ElementClickInterceptedException ex) { log.warn("Clic de cookies interceptado, usando JS."); ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn); }
                log.info("Cookies aceptadas.");
                popupHandled = true;
                driver.switchTo().defaultContent();
                shortWait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("iframe[title='SP Consent Message']")));
                log.debug("Iframe de cookies desaparecido.");
            } catch (Exception e) {
                log.debug("Banner de cookies no encontrado o error.");
                try { driver.switchTo().defaultContent(); log.debug("Asegurado: Volviendo al contenido principal."); } catch (NoSuchFrameException nfex) { log.trace("Ya estábamos en defaultContent."); }
            }
            // Pausa explícita DESPUÉS de manejar popups
            if (popupHandled) {
                log.debug("Aplicando pausa de estabilización post-popup...");
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                log.debug("Pausa de estabilización completada.");
            } else { log.debug("No se detectaron popups."); }
            // --- Fin Manejo de Pop-ups ---


            // --- SECCIÓN ELIMINADA: Selección de LaLiga ---
            log.info("Omitiendo selección de torneo. Procediendo a extraer la tabla inicial.");


            // --- Espera y Preparación ROBUSTA por la TABLA INICIAL ---
            WebElement tableBody = null;
            By tableBodyLocator = By.id("player-table-statistics-body");
            try {
                log.debug("Esperando que la tabla inicial (tbody) sea VISIBLE...");
                tableBody = wait.until(ExpectedConditions.visibilityOfElementLocated(tableBodyLocator));
                log.info("Tabla inicial (tbody) VISIBLE.");

                // Forzar scroll hacia la tabla
                try {
                    log.debug("Forzando scroll hacia la tabla...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", tableBody);
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {} // Pausa post-scroll
                    log.debug("Scroll hacia la tabla completado.");
                } catch (Exception scrollEx) {
                    log.warn("No se pudo forzar el scroll hacia la tabla: {}", scrollEx.getMessage());
                }

                // Esperar que contenga al menos una fila
                log.debug("Esperando que aparezca al menos una fila en la tabla inicial...");
                WebDriverWait rowWait = new WebDriverWait(driver, Duration.ofSeconds(30));
                // Esperar al menos una fila DENTRO del tbody visible
                rowWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.xpath(".//tr"), 0));
                log.debug("Al menos una fila detectada dentro de la tabla inicial.");

            } catch (TimeoutException e) {
                 // Timeout esperando visibilidad del tbody o presencia de filas
                 if (tableBody == null) { // Falló esperando visibilidad del tbody
                     log.error("¡ERROR CRÍTICO! Timeout esperando la visibilidad inicial del tbody: {}", e.getMessage(), e);
                     takeScreenshot(driver, baseScreenshotPath + "_initial_tbody_visibility_timeout.png");
                     throw new RuntimeException("Timeout crítico esperando la visibilidad inicial de la tabla.", e);
                 } else { // Falló esperando las filas dentro del tbody visible
                     log.warn("Timeout esperando filas (<tr>) dentro de la tabla inicial visible. La tabla podría estar vacía o tardando en poblarse.");
                     takeScreenshot(driver, baseScreenshotPath + "_no_rows_in_initial_table.png");
                     try { log.warn("Contenido HTML del tbody inicial en el momento del timeout de filas: {}", tableBody.getAttribute("innerHTML")); } catch (Exception ignored) {}
                     // No lanzar excepción aquí, simplemente no habrá jugadores
                 }
            } catch (NoSuchElementException nse) {
                 // Esto no debería ocurrir si la espera de visibilidad funcionó, pero por si acaso
                 log.error("Error crítico: No se encontró el elemento #player-table-statistics-body después de la espera de visibilidad.", nse);
                 takeScreenshot(driver, baseScreenshotPath + "_initial_tbody_not_found_post_wait.png");
                 throw new RuntimeException("No se pudo encontrar el tbody inicial para extraer filas.", nse);
            }


            // Extracción ROBUSTA (Manejo de StaleElement en el bucle)
            log.info("Procediendo a extraer jugadores de la tabla inicial...");
            // Volver a buscar las filas por si el DOM cambió durante las esperas/scroll
            List<WebElement> rows = driver.findElements(By.cssSelector("#player-table-statistics-body tr"));
            log.info("Filas encontradas en tabla inicial para procesar: {}", rows.size());
            int staleRowCount = 0; // Contador para filas obsoletas

             for (int i = 0; i < rows.size(); i++) { // Usar índice para logging si es stale
                WebElement row = rows.get(i);
                try { // --- INICIO TRY para StaleElement ---
                    List<WebElement> cols = row.findElements(By.tagName("td"));
                    if (cols.size() < 15) {
                        log.trace("Fila {} omitida, columnas insuficientes: {}", i, cols.size());
                        continue;
                    }

                    String name;
                    // Intentar extraer nombre (manejo de NoSuchElement interno)
                    try {
                        WebElement nameSpan = cols.get(0).findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
                        name = nameSpan.getText().trim();
                        if (name.isBlank()) {
                            name = cols.get(0).findElement(By.cssSelector("a.player-link")).getText().trim();
                            log.trace("Fila {}: Nombre obtenido por fallback de link: {}", i, name);
                        } else {
                            log.trace("Fila {}: Nombre obtenido del span: {}", i, name);
                        }
                    } catch (NoSuchElementException ex) {
                        String cellText = cols.get(0).getText();
                        String[] parts = cellText.split("\\n");
                        name = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                        log.trace("Fila {}: Nombre obtenido por fallback de texto directo: '{}' de '{}'", i, name, cellText);
                    }

                    if (name.isBlank()) {
                        log.warn("Fila {}: Nombre vacío detectado después de todos los fallbacks, fila omitida. Contenido celda[0]: {}", i, cols.get(0).getText());
                        continue;
                    }

                    // Extraer otros datos
                    PlayerBarcelona p = new PlayerBarcelona();
                    p.setName(name);
                    p.setMatches(cols.get(4).getText().trim());
                    p.setGoals(parseIntSafe(cols.get(6).getText()));
                    p.setAssists(parseIntSafe(cols.get(7).getText()));
                    p.setRating(parseDoubleSafe(cols.get(14).getText()));
                    players.add(p);
                    log.trace("Fila {}: Jugador procesado: {}", i, p.getName());

                } catch (StaleElementReferenceException e) { // --- CAPTURA StaleElement ---
                    staleRowCount++;
                    log.warn("⚠️ Fila {} se volvió obsoleta (StaleElementReferenceException) durante el procesamiento. Saltando esta fila. Error: {}", i, e.getMessage());
                    // Opcional: Podrías intentar re-localizar la fila y reintentar, pero por simplicidad la saltamos.
                    continue; // Saltar a la siguiente fila
                } // --- FIN TRY para StaleElement ---
            } // Fin del bucle for

            if (staleRowCount > 0) {
                log.warn("Se encontraron {} filas obsoletas (stale) durante el procesamiento.", staleRowCount);
            }

            // Guardar jugadores encontrados
            if (!players.isEmpty()) {
                log.info("Guardando {} jugadores...", players.size());
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else if (rows.isEmpty() && staleRowCount == 0) {
                 log.warn("⚠️ La tabla inicial no contenía filas de jugadores.");
                 // No es necesariamente un error si la tabla está vacía por defecto
            } else if (staleRowCount == rows.size() && !rows.isEmpty()) {
                 log.error("❌ Todas las {} filas encontradas se volvieron obsoletas. No se procesaron jugadores.", rows.size());
                 takeScreenshot(driver, baseScreenshotPath + "_all_rows_stale.png");
            }
            else {
                log.warn("⚠️ No se procesaron jugadores válidos de la tabla inicial ({} filas encontradas, {} stale).", rows.size(), staleRowCount);
                takeScreenshot(driver, baseScreenshotPath + "_no_valid_players_processed_initial.png");
            }

        } catch (TimeoutException e) {
            String waitInfo = (wait != null) ? wait.toString() : "N/A";
            String currentUrl = "N/A";
            if (driver != null) { try { currentUrl = driver.getCurrentUrl(); } catch (Exception urlEx) { currentUrl = "Error al obtener URL: " + urlEx.getMessage(); } }
            log.error("Timeout general ({}) esperando un elemento. URL actual: {}. Error: {}", waitInfo, currentUrl, e.getMessage(), e);
            takeScreenshot(driver, baseScreenshotPath + "_general_timeout_error.png");
        } catch (WebDriverException e) {
             if (e.getMessage() != null && e.getMessage().contains("DevToolsActivePort")) {
                 log.error("Error CRÍTICO de WebDriver al iniciar Chrome: {}. Causa probable: Recursos insuficientes (RAM, /dev/shm) o crash de Chrome.", e.getMessage(), e);
             } else if (e.getMessage() != null && e.getMessage().contains("session deleted or not found")) {
                 log.error("Error de WebDriver: La sesión parece haberse cerrado inesperadamente. {}", e.getMessage(), e);
             } else {
                 log.error("Error de WebDriver: {}", e.getMessage(), e);
             }
             takeScreenshot(driver, baseScreenshotPath + "_webdriver_error.png");
        } catch (RuntimeException e) {
             log.error("Scraping detenido debido a error previo: {}", e.getMessage());
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
    } // FIN MÉTODO scrapeAndSavePlayers


    // --- Métodos auxiliares (Sin cambios) ---
    private int parseIntSafe(String txt) { // INICIO MÉTODO parseIntSafe
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0;
        try {
            String numPart = txt.split("\\(")[0].trim();
            String digits = numPart.replaceAll("[^\\d]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            log.warn("parseIntSafe falló para '{}': {}", txt, e.getMessage());
            return 0;
        }
    } // FIN MÉTODO parseIntSafe

    private double parseDoubleSafe(String txt) { // INICIO MÉTODO parseDoubleSafe
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0.0;
        try {
            String cleaned = txt.replace(",", ".");
            cleaned = cleaned.replaceAll("[^\\d.]", "");
            if (cleaned.isEmpty() || cleaned.equals(".")) return 0.0;
            int firstDot = cleaned.indexOf('.');
            if (firstDot != -1) {
                int secondDot = cleaned.indexOf('.', firstDot + 1);
                if (secondDot != -1) { cleaned = cleaned.substring(0, secondDot); }
            }
             if (cleaned.startsWith(".")) cleaned = "0" + cleaned;
             if (cleaned.endsWith(".")) cleaned = cleaned.substring(0, cleaned.length() - 1);
             if (cleaned.isEmpty()) return 0.0;
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
             log.warn("parseDoubleSafe falló para '{}' (NumberFormatException): {}", txt, e.getMessage());
             return 0.0;
        } catch (Exception e) {
            log.warn("parseDoubleSafe falló para '{}' (Exception): {}", txt, e.getMessage());
            return 0.0;
        }
    } // FIN MÉTODO parseDoubleSafe

    private void takeScreenshot(WebDriver driver, String path) { // INICIO MÉTODO takeScreenshot
        if (driver instanceof TakesScreenshot) {
            try {
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
    } // FIN MÉTODO takeScreenshot

} // FIN CLASE ScraperServicePlayers
