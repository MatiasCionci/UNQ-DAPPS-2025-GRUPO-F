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
    private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration SWEETALERT_TIMEOUT = Duration.ofSeconds(6);
    private static final Duration TABLE_PRESENCE_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration TABLE_CONTENT_TIMEOUT = Duration.ofSeconds(45);

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // Helper para obtener textContent vía JS
    private String getTextContent(WebDriver driver, WebElement element) {
        try {
            JavascriptExecutor executor = (JavascriptExecutor) driver;
            return (String) executor.executeScript("return arguments[0].textContent;", element);
        } catch (Exception e) {
            log.warn("Error al obtener textContent vía JS para el elemento: {}", e.getMessage());
            // Fallback a getText() normal si JS falla
            try {
                return element.getText();
            } catch (Exception getTextEx) {
                log.error("Error también al obtener getText() como fallback: {}", getTextEx.getMessage());
                return ""; // Devolver vacío si todo falla
            }
        }
    }


    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() { // INICIO MÉTODO scrapeAndSavePlayers
        // ACTUALIZACIÓN v18.6: Usando JS textContent para extracción robusta contra <font> tags.
        log.info("🚀 Iniciando scraping de jugadores del Barcelona (v18.6 - JS textContent)...");
        List<PlayerBarcelona> players = new ArrayList<>();
        WebDriver driver = null;
        DevTools devTools = null;

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
            // Considerar añadir "--lang=es-ES,es" para influir en el idioma, aunque no garantiza nada si hay traducción JS
             ,"--lang=es-ES,es"
        );

        try {
            log.info("Inicializando ChromeDriver (v18.6)...");
            driver = new ChromeDriver(options);
            log.info("ChromeDriver inicializado correctamente.");

            // Configurar Bloqueo de Recursos con DevTools (usando v135)
            if (driver instanceof HasDevTools) {
                devTools = ((HasDevTools) driver).getDevTools();
                devTools.createSession();
                devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
                devTools.send(Network.setBlockedURLs(List.of(
                        "*.css", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.svg", "*.woff", "*.woff2", "*.ttf", "*.eot"
                        // Evitar bloquear JS esencial para la tabla
                )));
                log.info("Bloqueo de recursos via DevTools v135 habilitado.");
            } else {
                log.warn("El WebDriver actual no soporta DevTools.");
            }

            log.info("Navegando a la página...");
            driver.get("https://www.whoscored.com/teams/65/show/spain-barcelona");
            log.info("Página solicitada (PageLoadStrategy EAGER).");

            // Esperar explícitamente a document.readyState === 'complete'
            try {
                WebDriverWait readyWait = new WebDriverWait(driver, PAGE_LOAD_TIMEOUT);
                log.debug("Esperando document.readyState === 'complete' (max {}s)...", PAGE_LOAD_TIMEOUT.getSeconds());
                readyWait.until(drv -> ((JavascriptExecutor) drv).executeScript("return document.readyState").equals("complete"));
                log.info("✅ document.readyState es 'complete'.");
            } catch (TimeoutException e) {
                log.warn("⚠️ Timeout esperando document.readyState === 'complete'. Estado actual: {}",
                         (driver != null ? ((JavascriptExecutor) driver).executeScript("return document.readyState") : "N/A"));
            }

            // --- Manejo de Pop-ups (SweetAlert 6s max, no fail; No Cookies) ---
            boolean sweetAlertClosed = false;
            try {
                WebDriverWait sweetAlertWait = new WebDriverWait(driver, SWEETALERT_TIMEOUT);
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
            log.info("Manejo del banner de cookies OMITIDO (v18.6).");
            if (sweetAlertClosed) {
                log.debug("Aplicando pausa de estabilización post-cierre SweetAlert...");
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                log.debug("Pausa de estabilización completada.");
            }
            // --- Fin Manejo de Pop-ups ---

            log.info("Selección de Liga OMITIDA (v18.6).");

            // --- Espera Tabla (Presencia + Contenido) ---
            By tableBodyLocator = By.id("player-table-statistics-body");
            By rowsLocator = By.cssSelector("#player-table-statistics-body tr");
            WebElement tableBody = null;
            try {
                WebDriverWait tablePresenceWait = new WebDriverWait(driver, TABLE_PRESENCE_TIMEOUT);
                log.debug("Esperando que el tbody esté PRESENTE (max {}s)...", TABLE_PRESENCE_TIMEOUT.getSeconds());
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

                WebDriverWait tableContentWait = new WebDriverWait(driver, TABLE_CONTENT_TIMEOUT);
                log.debug("Esperando que al menos una fila (tr) esté PRESENTE dentro del tbody (max {}s)...", TABLE_CONTENT_TIMEOUT.getSeconds());
                tableContentWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(rowsLocator, 0));
                log.info("Al menos una fila (tr) PRESENTE dentro del tbody.");

            } catch (TimeoutException e) {
                 log.error("¡ERROR CRÍTICO! Timeout esperando PRESENCIA tbody ({}s) o filas ({}s): {}",
                           TABLE_PRESENCE_TIMEOUT.getSeconds(), TABLE_CONTENT_TIMEOUT.getSeconds(), e.getMessage(), e);
                 takeScreenshot(driver, baseScreenshotPath + "_tbody_or_rows_presence_timeout_v18.6.png");
                 try {
                     String bodyHtml = driver.findElement(By.tagName("body")).getAttribute("outerHTML");
                     log.error("HTML del body en el momento del timeout:\n{}", bodyHtml.substring(0, Math.min(bodyHtml.length(), 5000)));
                 } catch (Exception htmlEx) { log.error("No se pudo obtener el HTML del body."); }
                 throw new RuntimeException("Timeout crítico esperando la presencia de la tabla o sus filas.", e);
            } catch (NoSuchElementException nse) {
                 log.error("Error crítico: No se encontró #player-table-statistics-body al buscar tabla/filas.", nse);
                 takeScreenshot(driver, baseScreenshotPath + "_tbody_not_found_presence_v18.6.png");
                 throw new RuntimeException("No se pudo encontrar el tbody inicial para extraer filas.", nse);
            }

            // Extracción ROBUSTA con textContent
            log.info("Procediendo a extraer jugadores de la tabla usando textContent...");
            List<WebElement> rows = driver.findElements(rowsLocator);
            log.info("Filas encontradas para procesar: {}", rows.size());
            int staleRowCount = 0;

             for (int i = 0; i < rows.size(); i++) {
                // Re-obtener la fila en cada iteración puede ayudar con StaleElement, aunque puede ser más lento
                // WebElement row = driver.findElements(rowsLocator).get(i);
                WebElement row = rows.get(i); // Usar la lista original por eficiencia
                try {
                    List<WebElement> cols = row.findElements(By.tagName("td"));
                    if (cols.isEmpty() || cols.size() < 15) {
                        log.trace("Fila {} omitida, cols: {} (insuficientes o vacías)", i, cols.size());
                        continue;
                    }

                    // --- CAMBIO: Extracción de Nombre con textContent ---
                    String name = "";
                    WebElement nameElementContainer = null;
                    try {
                        // Intentar localizar el contenedor del nombre (span dentro del link)
                        nameElementContainer = cols.get(0).findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
                        name = getTextContent(driver, nameElementContainer).trim(); // Usar helper JS
                        log.trace("Fila {}: Nombre obtenido (vía JS textContent del span): '{}'", i, name);
                    } catch (NoSuchElementException spanEx) {
                        log.warn("Fila {}: No se encontró el span del nombre (a.player-link span.iconize-icon-left). Intentando fallback con link.", i);
                        try {
                            // Fallback: Intentar con el link <a> completo
                            nameElementContainer = cols.get(0).findElement(By.cssSelector("a.player-link"));
                            name = getTextContent(driver, nameElementContainer).trim(); // Usar helper JS
                            log.trace("Fila {}: Nombre obtenido (vía JS textContent del link <a>): '{}'", i, name);
                        } catch (NoSuchElementException linkEx) {
                            log.error("Fila {}: No se encontró ni span ni link para el nombre. Omitiendo.", i);
                            continue; // Saltar esta fila si no hay contenedor de nombre
                        }
                    } catch (Exception e) {
                         log.error("Fila {}: Error inesperado al obtener nombre con JS textContent. Contenido celda[0]: {}", i, getTextContent(driver, cols.get(0)), e);
                         continue;
                    }

                    // Limpieza adicional del nombre si es necesario (ej. quitar edad/posición si el fallback lo incluyó)
                    // Esto es heurístico y puede necesitar ajustes
                    if (name.contains(",")) { // Asumiendo que la edad/posición viene después de una coma
                        name = name.substring(0, name.indexOf(',')).trim();
                        log.trace("Fila {}: Nombre limpiado (post-coma): '{}'", i, name);
                    }
                    // Quitar el número inicial si está presente (ej. "1 Lamine Yamal")
                    name = name.replaceAll("^\\d+\\s+", "").trim();
                     log.trace("Fila {}: Nombre limpiado (post-número): '{}'", i, name);


                    if (name.isBlank()) {
                        log.warn("Fila {}: Nombre vacío detectado después de JS y limpieza, omitida. Contenido celda[0]: {}", i, getTextContent(driver, cols.get(0)));
                        continue;
                    }
                    // --- FIN CAMBIO Nombre ---


                    // --- CAMBIO: Extracción de Datos con textContent ---
                    PlayerBarcelona p = new PlayerBarcelona();
                    p.setName(name);
                    // Usar helper JS para obtener texto de las celdas de datos
                    p.setMatches(getTextContent(driver, cols.get(4)).trim());
                    p.setGoals(parseIntSafe(getTextContent(driver, cols.get(6))));
                    p.setAssists(parseIntSafe(getTextContent(driver, cols.get(7))));
                    p.setRating(parseDoubleSafe(getTextContent(driver, cols.get(14))));
                    // --- FIN CAMBIO Datos ---

                    players.add(p);
                    // log.trace("Fila {}: Jugador procesado: {}", i, p.getName()); // Log menos verboso

                } catch (StaleElementReferenceException e) {
                    staleRowCount++;
                    log.warn("⚠️ Fila {} stale. Saltando.", i);
                    // Podríamos re-buscar la lista 'rows' aquí si esto ocurre frecuentemente
                    // rows = driver.findElements(rowsLocator);
                    // if (i < rows.size()) row = rows.get(i); else break; // Reajustar o salir
                    continue;
                } catch (IndexOutOfBoundsException ioobe) {
                    log.error("❌ Error de índice en Fila {}: {}. Contenido fila: {}", i, ioobe.getMessage(), getTextContent(driver, row));
                    continue;
                } catch (Exception ex) {
                    log.error("❌ Error inesperado procesando Fila {}: {}. Contenido fila: {}", i, ex.getMessage(), getTextContent(driver, row), ex);
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
                 takeScreenshot(driver, baseScreenshotPath + "_no_rows_found_v18.6.png");
            } else if (!rows.isEmpty() && staleRowCount == rows.size()) {
                 log.error("❌ Todas las {} filas encontradas se volvieron obsoletas.", rows.size());
                 takeScreenshot(driver, baseScreenshotPath + "_all_rows_stale_v18.6.png");
            }
            else {
                log.warn("⚠️ No se procesaron jugadores válidos ({} filas iniciales, {} stale).", rows.size(), staleRowCount);
                takeScreenshot(driver, baseScreenshotPath + "_no_valid_players_processed_v18.6.png");
            }

        } catch (TimeoutException e) { // Captura timeouts generales no manejados antes
            log.error("Timeout general no capturado previamente: {}", e.getMessage(), e);
            takeScreenshot(driver, baseScreenshotPath + "_general_unhandled_timeout_v18.6.png");
        } catch (WebDriverException e) { // (Manejo sin cambios)
             if (e.getMessage() != null && e.getMessage().contains("DevToolsActivePort")) {
                 log.error("Error CRÍTICO WebDriver al iniciar Chrome: {}. Causa probable: Recursos insuficientes (RAM, /dev/shm).", e.getMessage(), e);
             } else if (e.getMessage() != null && e.getMessage().contains("session deleted or not found")) {
                 log.error("Error WebDriver: Sesión cerrada inesperadamente. {}", e.getMessage(), e);
             } else {
                 log.error("Error WebDriver: {}", e.getMessage(), e);
             }
             takeScreenshot(driver, baseScreenshotPath + "_webdriver_error_v18.6.png");
        } catch (RuntimeException e) { // (Manejo sin cambios)
             log.error("Scraping detenido debido a error previo: {}", e.getMessage());
        } catch (Exception e) { // (Manejo sin cambios)
            log.error("Error general inesperado en scraping: {}", e.getMessage(), e);
            takeScreenshot(driver, baseScreenshotPath + "_unexpected_error_v18.6.png");
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

        log.info("🏁 Scraping finalizado (v18.6). Total procesados: {} jugadores.", players.size());
        return players;
    } // FIN MÉTODO scrapeAndSavePlayers


    // --- Métodos auxiliares (parseIntSafe, parseDoubleSafe, takeScreenshot sin cambios) ---
    private int parseIntSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0;
        try {
            String numPart = txt.split("\\(")[0].trim();
            // Usar replaceAll para limpiar cualquier cosa que no sea dígito (incluyendo <font> tags)
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
            // Usar replaceAll para limpiar cualquier cosa que no sea dígito o punto
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
