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
        // ACTUALIZACIÓN v13: Espera por presencia de tbody + presencia de filas (tr).
        //               ¡¡¡IMPORTANTE!!! RE-VERIFICA shm_size y RAM.
        log.info("🚀 Iniciando scraping de jugadores del Barcelona (v13 - wait for rows presence)...");
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
            log.info("Inicializando ChromeDriver (v13)...");
            driver = new ChromeDriver(options);
            log.info("ChromeDriver inicializado correctamente.");
            // Considerar aumentar aún más si 150s no bastan, pero el problema puede ser otro
            wait = new WebDriverWait(driver, Duration.ofSeconds(180)); // Aumentado a 3 minutos
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
                // Usar wait largo para asegurar que desaparezca antes de continuar
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
                // Esperar a que el iframe desaparezca
                shortWait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("iframe[title='SP Consent Message']")));
                log.debug("Iframe de cookies desaparecido.");
            } catch (Exception e) {
                log.debug("Banner de cookies no encontrado o error.");
                try { driver.switchTo().defaultContent(); log.debug("Asegurado: Volviendo al contenido principal."); } catch (NoSuchFrameException nfex) { log.trace("Ya estábamos en defaultContent."); }
            }

            // --- CAMBIO: Screenshot DESPUÉS de manejar popups ---
            log.debug("Tomando screenshot DESPUÉS de manejar popups y volver a defaultContent...");
            takeScreenshot(driver, baseScreenshotPath + "_after_popups_v13.png");

            // Pausa explícita DESPUÉS de manejar popups (mantener)
            if (popupHandled) {
                log.debug("Aplicando pausa de estabilización post-popup...");
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                log.debug("Pausa de estabilización completada.");
            } else { log.debug("No se detectaron popups."); }
            // --- Fin Manejo de Pop-ups ---


            // --- SECCIÓN ELIMINADA: Selección de LaLiga ---
            log.info("Omitiendo selección de torneo. Procediendo a extraer la tabla inicial.");


            // --- CAMBIO: Espera por PRESENCIA de tbody y LUEGO por PRESENCIA de filas ---
            By tableBodyLocator = By.id("player-table-statistics-body");
            By rowsLocator = By.cssSelector("#player-table-statistics-body tr"); // Selector para filas DENTRO del tbody
            WebElement tableBody = null; // Para logging en caso de error de filas

            try {
                log.debug("Esperando que el contenedor de la tabla (tbody) esté PRESENTE en el DOM...");
                tableBody = wait.until(ExpectedConditions.presenceOfElementLocated(tableBodyLocator));
                log.info("Contenedor de tabla (tbody) PRESENTE.");

                // Ahora, esperar que al menos una fila (tr) esté PRESENTE DENTRO del tbody
                log.debug("Esperando que al menos una fila (tr) esté PRESENTE dentro del tbody...");
                // Usamos presenceOfAllElementsLocatedBy que devuelve una lista no vacía si encuentra al menos uno
                wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(rowsLocator));
                log.info("Al menos una fila (tr) PRESENTE dentro del tbody.");

                // Opcional: Scroll a la tabla ahora que sabemos que tiene filas (o al menos el tbody existe)
                try {
                    log.debug("Forzando scroll hacia la tabla...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", tableBody);
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    log.debug("Scroll hacia la tabla completado.");
                } catch (Exception scrollEx) {
                    log.warn("No se pudo forzar el scroll hacia la tabla: {}", scrollEx.getMessage());
                }

            } catch (TimeoutException e) {
                 // Si falla aquí, es más grave, porque ni siquiera aparecen las filas en el DOM
                 log.error("¡ERROR CRÍTICO! Timeout esperando la PRESENCIA del tbody o de las filas (tr) dentro de él: {}", e.getMessage(), e);
                 takeScreenshot(driver, baseScreenshotPath + "_tbody_or_rows_presence_timeout.png");
                 // Intentar obtener HTML del body para depurar
                 try {
                     String bodyHtml = driver.findElement(By.tagName("body")).getAttribute("outerHTML");
                     log.error("HTML del body en el momento del timeout:\n{}", bodyHtml.substring(0, Math.min(bodyHtml.length(), 5000))); // Loguear primeros 5KB
                 } catch (Exception htmlEx) {
                     log.error("No se pudo obtener el HTML del body.");
                 }
                 throw new RuntimeException("Timeout crítico esperando la presencia de la tabla o sus filas.", e);
            } catch (NoSuchElementException nse) {
                 // Si presenceOfElementLocated fallara de forma inesperada
                 log.error("Error crítico: No se encontró el elemento #player-table-statistics-body incluso esperando presencia.", nse);
                 takeScreenshot(driver, baseScreenshotPath + "_initial_tbody_not_found_presence.png");
                 throw new RuntimeException("No se pudo encontrar el tbody inicial para extraer filas.", nse);
            }


            // Extracción ROBUSTA (Manejo de StaleElement en el bucle - Sin cambios v12)
            log.info("Procediendo a extraer jugadores de la tabla inicial...");
            List<WebElement> rows = driver.findElements(rowsLocator); // Usar el selector de filas ya definido
            log.info("Filas encontradas en tabla inicial para procesar: {}", rows.size());
            int staleRowCount = 0;

             for (int i = 0; i < rows.size(); i++) {
                WebElement row = rows.get(i);
                try {
                    List<WebElement> cols = row.findElements(By.tagName("td"));
                    if (cols.size() < 15) {
                        log.trace("Fila {} omitida, columnas insuficientes: {}", i, cols.size());
                        continue;
                    }
                    String name;
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
                        log.warn("Fila {}: Nombre vacío detectado, fila omitida. Contenido celda[0]: {}", i, cols.get(0).getText());
                        continue;
                    }
                    PlayerBarcelona p = new PlayerBarcelona();
                    p.setName(name);
                    p.setMatches(cols.get(4).getText().trim());
                    p.setGoals(parseIntSafe(cols.get(6).getText()));
                    p.setAssists(parseIntSafe(cols.get(7).getText()));
                    p.setRating(parseDoubleSafe(cols.get(14).getText()));
                    players.add(p);
                    log.trace("Fila {}: Jugador procesado: {}", i, p.getName());

                } catch (StaleElementReferenceException e) {
                    staleRowCount++;
                    log.warn("⚠️ Fila {} se volvió obsoleta (StaleElementReferenceException) durante el procesamiento. Saltando. Error: {}", i, e.getMessage());
                    continue;
                }
            }

            if (staleRowCount > 0) { log.warn("Se encontraron {} filas obsoletas (stale).", staleRowCount); }

            // Guardar jugadores
            if (!players.isEmpty()) {
                log.info("Guardando {} jugadores...", players.size());
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else if (rows.isEmpty() && staleRowCount == 0) {
                 log.warn("⚠️ La tabla inicial no contenía filas de jugadores.");
            } else if (staleRowCount == rows.size() && !rows.isEmpty()) {
                 log.error("❌ Todas las {} filas encontradas se volvieron obsoletas.", rows.size());
                 takeScreenshot(driver, baseScreenshotPath + "_all_rows_stale.png");
            }
            else {
                log.warn("⚠️ No se procesaron jugadores válidos ({} filas encontradas, {} stale).", rows.size(), staleRowCount);
                takeScreenshot(driver, baseScreenshotPath + "_no_valid_players_processed_initial.png");
            }

        } catch (TimeoutException e) {
            String waitInfo = (wait != null) ? wait.toString() : "N/A";
            String currentUrl = "N/A";
            if (driver != null) { try { currentUrl = driver.getCurrentUrl(); } catch (Exception urlEx) { currentUrl = "Error al obtener URL: " + urlEx.getMessage(); } }
            // Distinguir si el timeout fue esperando la tabla/filas o fue otro general
            if (e.getMessage() != null && (e.getMessage().contains("player-table-statistics-body") || e.getMessage().contains("tr"))) {
                 // El error ya se logueó dentro del try/catch específico de la tabla
                 log.error("El TimeoutException ocurrió esperando la tabla o sus filas (ver logs anteriores).");
            } else {
                 log.error("Timeout general ({}) esperando un elemento. URL actual: {}. Error: {}", waitInfo, currentUrl, e.getMessage(), e);
                 takeScreenshot(driver, baseScreenshotPath + "_general_timeout_error.png");
            }
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
