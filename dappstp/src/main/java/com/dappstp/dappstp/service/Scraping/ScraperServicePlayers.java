package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.repository.PlayerBarcelonaRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v135.network.Network; // Usando v135 según POM
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.PageLoadStrategy;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
@Slf4j
public class ScraperServicePlayers { // INICIO CLASE

    private final PlayerBarcelonaRepository playerRepository;
    private final String baseScreenshotPath = "/app/screenshot";
    // Definir timeouts para claridad y fácil ajuste
    private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(90); // Timeout para carga inicial y readyState
    private static final Duration SWEETALERT_TIMEOUT = Duration.ofSeconds(6); // Timeout corto para SweetAlert
    private static final Duration TABLE_PRESENCE_TIMEOUT = Duration.ofSeconds(60); // Timeout para que exista el tbody
    private static final Duration TABLE_CONTENT_TIMEOUT = Duration.ofSeconds(45); // Timeout para que tbody tenga filas (tr)

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() { // INICIO MÉTODO scrapeAndSavePlayers
        // ACTUALIZACIÓN v18.5: SweetAlert(6s max, no fail), no cookies, SIN selección de Liga.
        log.info("🚀 Iniciando scraping de jugadores del Barcelona (v18.5 - SweetAlert(6s), no cookies, no Liga select)...");
        List<PlayerBarcelona> players = new ArrayList<>();
        WebDriver driver = null;
        DevTools devTools = null;
        // No necesitamos el 'wait' general largo si usamos waits específicos
        // WebDriverWait wait = null;

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        log.info("PageLoadStrategy configurado en EAGER.");

        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--disable-extensions",
            "--window-size=1280,800"
        );

        try {
            log.info("Inicializando ChromeDriver (v18.5)...");
            driver = new ChromeDriver(options);
            log.info("ChromeDriver inicializado correctamente.");

            // Configurar Bloqueo de Recursos con DevTools (usando v135)
            if (driver instanceof HasDevTools) {
                devTools = ((HasDevTools) driver).getDevTools();
                devTools.createSession();
                devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
                devTools.send(Network.setBlockedURLs(List.of(
                        "*.css", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.svg", "*.woff", "*.woff2", "*.ttf", "*.eot"
                )));
                log.info("Bloqueo de recursos via DevTools v135 habilitado (CSS, Fonts, Images, etc.).");
            } else {
                log.warn("El WebDriver actual no soporta DevTools. El bloqueo de recursos será limitado.");
            }

            log.info("Navegando a la página...");
            driver.get("https://www.whoscored.com/teams/65/show/spain-barcelona");
            log.info("Página solicitada (PageLoadStrategy EAGER).");

            // Esperar explícitamente a document.readyState === 'complete'
            try {
                WebDriverWait readyWait = new WebDriverWait(driver, PAGE_LOAD_TIMEOUT); // Usar timeout de carga de página
                log.debug("Esperando document.readyState === 'complete' (max {}s)...", PAGE_LOAD_TIMEOUT.getSeconds());
                readyWait.until(drv -> ((JavascriptExecutor) drv).executeScript("return document.readyState").equals("complete"));
                log.info("✅ document.readyState es 'complete'.");
            } catch (TimeoutException e) {
                log.warn("⚠️ Timeout esperando document.readyState === 'complete'. Estado actual: {}",
                         (driver != null ? ((JavascriptExecutor) driver).executeScript("return document.readyState") : "N/A"));
                // Continuar de todas formas
            }

            // --- Manejo de Pop-ups MODIFICADO ---
            boolean sweetAlertClosed = false;
            // SweetAlert: Intentar cerrar por 6 segundos, NO fallar si no se puede.
            try {
                WebDriverWait sweetAlertWait = new WebDriverWait(driver, SWEETALERT_TIMEOUT); // Usar timeout específico
                log.debug("Buscando SweetAlert (max {}s)...", SWEETALERT_TIMEOUT.getSeconds());
                By swalClose = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
                WebElement btn = sweetAlertWait.until(ExpectedConditions.visibilityOfElementLocated(swalClose));
                log.debug("SweetAlert encontrado, intentando cerrar...");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                sweetAlertWait.until(ExpectedConditions.invisibilityOfElementLocated(swalClose));
                log.info("SweetAlert cerrado con éxito.");
                sweetAlertClosed = true;
            } catch (TimeoutException e) {
                log.info("SweetAlert no encontrado o no cerrado en {}s. Continuando...", SWEETALERT_TIMEOUT.getSeconds());
            } catch (Exception e) {
                log.warn("Error inesperado al intentar cerrar SweetAlert (continuando): {}", e.getMessage());
            }

            // Banner de Cookies: ELIMINADO
            log.info("Manejo del banner de cookies OMITIDO (v18.5).");

            // Pausa opcional si el SweetAlert FUE cerrado
            if (sweetAlertClosed) {
                log.debug("Aplicando pausa de estabilización post-cierre SweetAlert...");
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                log.debug("Pausa de estabilización completada.");
            }
            // --- Fin Manejo de Pop-ups ---


            // --- CAMBIO: Selección de Liga ELIMINADA ---
            log.info("Selección de Liga OMITIDA (v18.5). Asumiendo que la URL base es suficiente.");
            // --- FIN CAMBIO ---


            // --- Espera Tabla (Directamente después de popups) ---
            By tableBodyLocator = By.id("player-table-statistics-body");
            By rowsLocator = By.cssSelector("#player-table-statistics-body tr");
            WebElement tableBody = null;

            try {
                // Usar waits específicos para la tabla
                WebDriverWait tablePresenceWait = new WebDriverWait(driver, TABLE_PRESENCE_TIMEOUT);
                log.debug("Esperando que el contenedor de la tabla (tbody) esté PRESENTE (max {}s)...", TABLE_PRESENCE_TIMEOUT.getSeconds());
                tableBody = tablePresenceWait.until(ExpectedConditions.presenceOfElementLocated(tableBodyLocator));
                log.info("Contenedor de tabla (tbody) PRESENTE.");

                // Scroll opcional
                try {
                    log.debug("Forzando scroll hacia la tabla...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", tableBody);
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    log.debug("Scroll hacia la tabla completado.");
                } catch (Exception scrollEx) {
                    log.warn("No se pudo forzar el scroll hacia la tabla: {}", scrollEx.getMessage());
                }

                // Esperar contenido (filas)
                WebDriverWait tableContentWait = new WebDriverWait(driver, TABLE_CONTENT_TIMEOUT);
                log.debug("Esperando que al menos una fila (tr) esté PRESENTE dentro del tbody (max {}s)...", TABLE_CONTENT_TIMEOUT.getSeconds());
                tableContentWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(rowsLocator, 0));
                log.info("Al menos una fila (tr) PRESENTE dentro del tbody.");

            } catch (TimeoutException e) {
                 log.error("¡ERROR CRÍTICO! Timeout esperando la PRESENCIA del tbody ({}s) o de las filas ({}s): {}",
                           TABLE_PRESENCE_TIMEOUT.getSeconds(), TABLE_CONTENT_TIMEOUT.getSeconds(), e.getMessage(), e);
                 takeScreenshot(driver, baseScreenshotPath + "_tbody_or_rows_presence_timeout_v18.5.png");
                 try {
                     String bodyHtml = driver.findElement(By.tagName("body")).getAttribute("outerHTML");
                     log.error("HTML del body en el momento del timeout:\n{}", bodyHtml.substring(0, Math.min(bodyHtml.length(), 5000)));
                 } catch (Exception htmlEx) { log.error("No se pudo obtener el HTML del body."); }
                 throw new RuntimeException("Timeout crítico esperando la presencia de la tabla o sus filas.", e);
            } catch (NoSuchElementException nse) {
                 log.error("Error crítico: No se encontró #player-table-statistics-body al buscar tabla/filas.", nse);
                 takeScreenshot(driver, baseScreenshotPath + "_tbody_not_found_presence_v18.5.png");
                 throw new RuntimeException("No se pudo encontrar el tbody inicial para extraer filas.", nse);
            }

            // Extracción ROBUSTA (Sin cambios en la lógica interna del bucle)
            log.info("Procediendo a extraer jugadores de la tabla...");
            List<WebElement> rows = driver.findElements(rowsLocator); // Re-buscar filas
            log.info("Filas encontradas para procesar: {}", rows.size());
            int staleRowCount = 0;

             for (int i = 0; i < rows.size(); i++) {
                WebElement row = rows.get(i);
                try {
                    List<WebElement> cols = row.findElements(By.tagName("td"));
                    if (cols.isEmpty() || cols.size() < 15) {
                        log.trace("Fila {} omitida, cols: {} (insuficientes o vacías)", i, cols.size());
                        continue;
                    }
                    // ... (lógica de extracción de nombre y datos sin cambios) ...
                     String name;
                    WebElement firstCol = cols.get(0);
                    try {
                        WebElement nameSpan = firstCol.findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
                        name = nameSpan.getText().trim();
                        if (name.isBlank()) {
                            name = firstCol.findElement(By.cssSelector("a.player-link")).getText().trim();
                            log.trace("Fila {}: Nombre fallback link (span vacío): {}", i, name);
                        } else {
                            log.trace("Fila {}: Nombre span: {}", i, name);
                        }
                    } catch (NoSuchElementException ex) {
                        String cellText = firstCol.getText();
                        String[] parts = cellText.split("\\n");
                        name = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                        log.trace("Fila {}: Nombre fallback texto: '{}' de '{}'", i, name, cellText);
                    }

                    if (name.isBlank()) {
                        log.warn("Fila {}: Nombre vacío detectado después de fallbacks, omitida. Celda[0]: {}", i, firstCol.getText());
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
                    log.warn("⚠️ Fila {} stale. Saltando.", i);
                    continue;
                } catch (IndexOutOfBoundsException ioobe) {
                    log.error("❌ Error de índice en Fila {}: {}. Contenido fila: {}", i, ioobe.getMessage(), row.getText());
                    continue;
                } catch (Exception ex) {
                    log.error("❌ Error inesperado procesando Fila {}: {}. Contenido fila: {}", i, ex.getMessage(), row.getText(), ex);
                    continue;
                }
            } // Fin del bucle for

            if (staleRowCount > 0) { log.warn("Se encontraron {} filas obsoletas (stale) durante la extracción.", staleRowCount); }

            // Guardar jugadores (Sin cambios)
            if (!players.isEmpty()) {
                log.info("Guardando {} jugadores...", players.size());
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else if (rows.isEmpty() && staleRowCount == 0) {
                 log.warn("⚠️ La tabla no contenía filas de jugadores.");
                 takeScreenshot(driver, baseScreenshotPath + "_no_rows_found_v18.5.png");
            } else if (!rows.isEmpty() && staleRowCount == rows.size()) {
                 log.error("❌ Todas las {} filas encontradas se volvieron obsoletas.", rows.size());
                 takeScreenshot(driver, baseScreenshotPath + "_all_rows_stale_v18.5.png");
            }
            else {
                log.warn("⚠️ No se procesaron jugadores válidos ({} filas iniciales, {} stale).", rows.size(), staleRowCount);
                takeScreenshot(driver, baseScreenshotPath + "_no_valid_players_processed_v18.5.png");
            }

        } catch (TimeoutException e) { // Captura timeouts generales no manejados antes
            log.error("Timeout general no capturado previamente: {}", e.getMessage(), e);
            takeScreenshot(driver, baseScreenshotPath + "_general_unhandled_timeout_v18.5.png");
        } catch (WebDriverException e) { // (Manejo sin cambios)
             if (e.getMessage() != null && e.getMessage().contains("DevToolsActivePort")) {
                 log.error("Error CRÍTICO WebDriver al iniciar Chrome: {}. Causa probable: Recursos insuficientes (RAM, /dev/shm).", e.getMessage(), e);
             } else if (e.getMessage() != null && e.getMessage().contains("session deleted or not found")) {
                 log.error("Error WebDriver: Sesión cerrada inesperadamente. {}", e.getMessage(), e);
             } else {
                 log.error("Error WebDriver: {}", e.getMessage(), e);
             }
             takeScreenshot(driver, baseScreenshotPath + "_webdriver_error_v18.5.png");
        } catch (RuntimeException e) { // (Manejo sin cambios)
             log.error("Scraping detenido debido a error previo: {}", e.getMessage());
        } catch (Exception e) { // (Manejo sin cambios)
            log.error("Error general inesperado en scraping: {}", e.getMessage(), e);
            takeScreenshot(driver, baseScreenshotPath + "_unexpected_error_v18.5.png");
        } finally {
            // Cerrar sesión de DevTools (Sin cambios)
            if (devTools != null) {
                try {
                    log.debug("Cerrando sesión de DevTools...");
                    devTools.close();
                    log.debug("Sesión de DevTools cerrada.");
                } catch (Exception e) {
                    log.error("Error al cerrar sesión de DevTools: {}", e.getMessage());
                }
            }
            // Cerrar WebDriver (Sin cambios)
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

        log.info("🏁 Scraping finalizado (v18.5). Total procesados: {} jugadores.", players.size());
        return players;
    } // FIN MÉTODO scrapeAndSavePlayers


    // --- Métodos auxiliares (Sin cambios) ---
    private int parseIntSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0;
        try {
            String numPart = txt.split("\\(")[0].trim();
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
    }

    private void takeScreenshot(WebDriver driver, String path) {
        if (driver instanceof TakesScreenshot) {
            try {
                byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                try (FileOutputStream fos = new FileOutputStream(path)) {
                    fos.write(bytes);
                    log.warn("📸 Captura guardada en: {}", path);
                }
            } catch (WebDriverException wde) {
                 if (wde.getMessage() != null && wde.getMessage().contains("session deleted or not found")) {
                     log.error("No se pudo tomar screenshot para '{}' porque la sesión del WebDriver ya estaba cerrada.", path);
                 } else {
                     log.error("Error de WebDriver al tomar screenshot para '{}': {}", path, wde.getMessage());
                 }
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

} // FIN CLASE ScraperServicePlayers
