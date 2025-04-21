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

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() { // INICIO MÉTODO scrapeAndSavePlayers
        // ACTUALIZACIÓN v9: Pausa post-popups + espera de visibilidad simple para selector.
        //               ¡¡¡IMPORTANTE!!! RE-VERIFICA shm_size y RAM.
        log.info("🚀 Iniciando scraping de jugadores del Barcelona (v9 - pause + visibility wait)...");
        List<PlayerBarcelona> players = new ArrayList<>();
        WebDriver driver = null;
        WebDriverWait wait = null;
        String baseScreenshotPath = "/app/screenshot";

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
            log.info("Inicializando ChromeDriver (v9)...");
            driver = new ChromeDriver(options);
            log.info("ChromeDriver inicializado correctamente.");
            wait = new WebDriverWait(driver, Duration.ofSeconds(150)); // Mantener timeout general largo
            log.info("Navegando a la página...");
            driver.get("https://www.whoscored.com/teams/65/show/spain-barcelona");
            log.info("Página cargada (según PageLoadStrategy NORMAL).");

            // --- Manejo de Pop-ups ---
            boolean popupHandled = false; // Bandera para saber si se manejó algún popup

            // SweetAlert
            try {
                log.debug("Buscando SweetAlert...");
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
                By swalClose = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
                WebElement btn = shortWait.until(ExpectedConditions.visibilityOfElementLocated(swalClose));
                log.debug("SweetAlert encontrado, intentando cerrar...");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(swalClose)); // Esperar a que desaparezca
                log.info("SweetAlert cerrado.");
                popupHandled = true;
            } catch (Exception e) {
                log.debug("SweetAlert no encontrado o ya cerrado (Timeout corto o error: {}).", e.getMessage());
            }

            // Cookies
            try {
                log.debug("Buscando banner de cookies...");
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(15));
                // Esperar iframe Y cambiar a él
                shortWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                    By.cssSelector("iframe[title='SP Consent Message']")));
                log.debug("Dentro del iframe de cookies.");
                By acceptBtn = By.xpath("//button[.//span[contains(., 'Accept')] or contains(., 'Accept') or .//span[contains(., 'Aceptar')] or contains(., 'Aceptar')]");
                // Esperar botón clicable DENTRO del iframe
                WebElement btn = shortWait.until(ExpectedConditions.elementToBeClickable(acceptBtn));
                log.debug("Botón Aceptar encontrado, intentando clic...");
                try {
                    btn.click();
                } catch (ElementClickInterceptedException ex) {
                    log.warn("Clic de cookies interceptado, usando JS.");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                }
                log.info("Cookies aceptadas.");
                popupHandled = true;
                // IMPORTANTE: Volver al contenido principal DESPUÉS de hacer clic
                driver.switchTo().defaultContent();
                // Esperar a que el iframe desaparezca del DOM principal
                shortWait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("iframe[title='SP Consent Message']")));
                log.debug("Iframe de cookies desaparecido.");
            } catch (Exception e) {
                log.debug("Banner de cookies no encontrado o error (Timeout corto o error: {}).", e.getMessage());
                // Asegurarse de volver al contenido principal incluso si falla
                try {
                    driver.switchTo().defaultContent();
                    log.debug("Asegurado: Volviendo al contenido principal después de error/no encontrar cookies.");
                } catch (NoSuchFrameException nfex) {
                    log.trace("Ya estábamos en defaultContent o iframe no existía.");
                }
            }
            // --- Fin Manejo de Pop-ups ---

            // --- CAMBIO: Pausa explícita DESPUÉS de manejar popups y volver a defaultContent ---
            if (popupHandled) {
                log.debug("Se manejó al menos un popup. Aplicando pausa de estabilización...");
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {} // Pausa de 3 segundos
                log.debug("Pausa de estabilización completada.");
            } else {
                log.debug("No se detectaron popups para manejar.");
            }


            // Selección de LaLiga (Lógica v9)
            log.info("Intentando seleccionar LaLiga...");
            WebElement selectElem = null;
            try {
                By torneoLocator = By.cssSelector("select[data-backbone-model-attribute-dd='tournamentOptions']");

                log.debug("Tomando screenshot ANTES de esperar por el selector (después de pausa post-popup)...");
                takeScreenshot(driver, baseScreenshotPath + "_before_select_wait_v9.png");

                // --- CAMBIO: Volver a esperar solo visibilidad ---
                log.debug("Esperando que el selector de torneo sea VISIBLE...");
                selectElem = wait.until(ExpectedConditions.visibilityOfElementLocated(torneoLocator));
                log.debug("Selector VISIBLE.");

                // --- Mantener Scroll ---
                try {
                    log.debug("Forzando scroll hacia el selector...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", selectElem);
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    log.debug("Scroll hacia el selector completado.");
                } catch (Exception scrollEx) {
                    log.warn("No se pudo forzar el scroll hacia el selector: {}", scrollEx.getMessage());
                }

                // --- Mantener Intento con Select y Fallback JS ---
                log.debug("Intentando seleccionar 'LaLiga' con la clase Select...");
                try {
                    new Select(selectElem).selectByVisibleText("LaLiga");
                    log.info("Opción 'LaLiga' seleccionada usando la clase Select.");
                } catch (ElementNotInteractableException | StaleElementReferenceException enie) {
                    log.warn("El selector era visible pero no interactuable/stale. Intentando seleccionar 'LaLiga' con JavaScript como fallback. Error original: {}", enie.getMessage());
                    if (enie instanceof StaleElementReferenceException) {
                         log.debug("Re-localizando elemento stale antes del fallback JS...");
                         try {
                             WebDriverWait shortFindWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                             selectElem = shortFindWait.until(ExpectedConditions.presenceOfElementLocated(torneoLocator));
                         } catch (Exception findEx) {
                              log.error("No se pudo re-localizar el elemento stale para el fallback JS: {}", findEx.getMessage());
                              throw new RuntimeException("Fallo crítico al re-localizar selector para fallback JS.", findEx);
                         }
                    }
                    if (selectElem == null) { throw new RuntimeException("Fallo crítico: selectElem es null antes del fallback JS."); }
                    try {
                        String script = "arguments[0].value = '8'; " + // Value para LaLiga, verificar si cambia
                                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));";
                        ((JavascriptExecutor) driver).executeScript(script, selectElem);
                        log.info("Opción 'LaLiga' seleccionada (o intentada) usando JavaScript.");
                        try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
                    } catch (Exception jsEx) {
                        log.error("¡FALLÓ también el fallback con JavaScript para seleccionar LaLiga! {}", jsEx.getMessage(), jsEx);
                        throw new RuntimeException("Fallo crítico al seleccionar LaLiga (ni Select ni JS funcionaron).", jsEx);
                    }
                }

                // --- Espera por la actualización de la tabla ---
                log.debug("Esperando que la tabla de jugadores sea VISIBLE después de seleccionar LaLiga...");
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("player-table-statistics-body")));
                log.info("Tabla de jugadores VISIBLE después de seleccionar LaLiga.");
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                log.debug("Pausa post-selección completada.");

            } catch (TimeoutException toe) {
                 // Timeout esperando visibilidad del selector
                 log.error("¡ERROR CRÍTICO! Timeout esperando que el selector de torneo fuera VISIBLE: {}. ¿Está el selector correcto? ¿Overlay? ¿Recursos?", toe.getMessage(), toe);
                 takeScreenshot(driver, baseScreenshotPath + "_select_visibility_timeout_v9.png"); // Screenshot específico
                 throw new RuntimeException("Timeout crítico esperando la visibilidad del selector de torneo.", toe);
            } catch (Exception e) {
                log.error("¡ERROR CRÍTICO! Fallo inesperado durante la selección de LaLiga: {}", e.getMessage(), e);
                takeScreenshot(driver, baseScreenshotPath + "_laliga_select_unexpected_error.png");
                throw new RuntimeException("Fallo crítico inesperado al procesar selección de LaLiga.", e);
            }


            // Extracción (Sin cambios)
            log.info("Procediendo a extraer jugadores de la tabla...");
            // ... (resto del código de extracción igual que antes) ...
            log.debug("Esperando que aparezca al menos una fila en la tabla...");
            WebElement tableBody = null;
            try {
                WebDriverWait rowWait = new WebDriverWait(driver, Duration.ofSeconds(45));
                tableBody = driver.findElement(By.id("player-table-statistics-body"));
                rowWait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.xpath(".//tr"), 0));
                log.debug("Al menos una fila detectada dentro de la tabla.");
            } catch (TimeoutException ex) {
                log.warn("Timeout esperando filas (<tr>) dentro de la tabla visible. La tabla podría estar vacía o tardando demasiado en poblarse.");
                takeScreenshot(driver, baseScreenshotPath + "_no_rows_in_table.png");
                if (tableBody != null) { try { log.warn("Contenido HTML del tbody en el momento del timeout de filas: {}", tableBody.getAttribute("innerHTML")); } catch (Exception ignored) {} }
            } catch (NoSuchElementException nse) {
                 log.error("Error crítico: No se encontró el elemento #player-table-statistics-body para buscar filas.", nse);
                 takeScreenshot(driver, baseScreenshotPath + "_tbody_not_found_for_rows.png");
                 throw new RuntimeException("No se pudo encontrar el tbody para extraer filas.", nse);
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
                    WebElement nameSpan = cols.get(0).findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
                    name = nameSpan.getText().trim();
                    if (name.isBlank()) {
                        name = cols.get(0).findElement(By.cssSelector("a.player-link")).getText().trim();
                        log.trace("Nombre obtenido por fallback de link (span vacío o en blanco): {}", name);
                    } else {
                         log.trace("Nombre obtenido del span: {}", name);
                    }
                } catch (NoSuchElementException ex) {
                    String cellText = cols.get(0).getText();
                    String[] parts = cellText.split("\\n");
                    name = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                    log.trace("Nombre obtenido por fallback de texto directo (split): '{}' de '{}'", name, cellText);
                }
                if (name.isBlank()) {
                    log.warn("Nombre vacío detectado después de todos los fallbacks, fila omitida. Contenido celda[0]: {}", cols.get(0).getText());
                    continue;
                }
                PlayerBarcelona p = new PlayerBarcelona();
                p.setName(name);
                p.setMatches(cols.get(4).getText().trim());
                p.setGoals(parseIntSafe(cols.get(6).getText()));
                p.setAssists(parseIntSafe(cols.get(7).getText()));
                p.setRating(parseDoubleSafe(cols.get(14).getText()));
                players.add(p);
                log.trace("Jugador procesado: {}", p.getName());
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
