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
import java.util.Map; // <-- AÑADIDO: Importar Map
import java.util.HashMap; // <-- AÑADIDO: Importar HashMap

@Service
@Slf4j
public class ScraperServicePlayers {

    private final PlayerBarcelonaRepository playerRepository;
    // Mover a campo para usar en takeScreenshot si se refactoriza
    private final String baseScreenshotPath = "/app/screenshot";

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() {
        // ACTUALIZACIÓN v16: Bloqueando imágenes para reducir recursos.
        //               Basado en el código v5 proporcionado.
        //               ¡¡¡IMPORTANTE!!! RE-VERIFICA shm_size y RAM.
        log.info("🚀 Iniciando scraping de jugadores del Barcelona (v16 - block images)...");
        List<PlayerBarcelona> players = new ArrayList<>();
        WebDriver driver = null;
        WebDriverWait wait = null;

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        // --- NUEVO: Configuración para bloquear imágenes ---
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.managed_default_content_settings.images", 2); // 2 = Block images
        // Opcional: Bloquear CSS (puede romper selectores):
        // prefs.put("profile.managed_default_content_settings.stylesheets", 2);
        // Opcional: Bloquear Fuentes:
        // prefs.put("profile.managed_default_content_settings.fonts", 2);
        options.setExperimentalOption("prefs", prefs);
        // --- FIN Bloqueo imágenes ---

        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            // "--disable-setuid-sandbox", // Redundante con no-sandbox
            "--disable-dev-shm-usage",    // Mantener, junto con shm_size externo
            "--disable-gpu",
            "--disable-extensions",       // Añadido en versiones anteriores, mantener
            "--window-size=1920,1080"
            // User-agent y user-data-dir comentados para simplificar (como en v9+)
            // "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.7049.95 Safari/537.36",
            // "--user-data-dir=/tmp/chrome-profile-" + UUID.randomUUID()
        );

        try {
            log.info("Inicializando ChromeDriver (v16 con bloqueo de imágenes)...");
            driver = new ChromeDriver(options);
            log.info("ChromeDriver inicializado correctamente.");
            // Usar timeout largo por si el entorno es lento
            wait = new WebDriverWait(driver, Duration.ofSeconds(180)); // 3 minutos
            log.info("Navegando a la página...");
            driver.get("https://www.whoscored.com/teams/65/show/spain-barcelona");
            log.info("Página cargada (según PageLoadStrategy NORMAL).");

            // --- Manejo de Pop-ups (Como en v9+) ---
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


            // --- Selección de LaLiga (Mantenida de v5, pero ahora con bloqueo de imágenes) ---
            // NOTA: Si esto sigue fallando, considera eliminar esta sección como en v11/v13
            log.info("Intentando seleccionar LaLiga...");
            try {
                By torneoLocator = By.cssSelector("select[data-backbone-model-attribute-dd='tournamentOptions']");

                // --- Usar la espera robusta v10 ---
                log.debug("Tomando screenshot ANTES de esperar por el selector (v16)...");
                takeScreenshot(driver, baseScreenshotPath + "_before_select_wait_v16.png");

                log.debug("Esperando que el selector de torneo esté presente, visible y habilitado (custom wait)...");
                WebElement selectElem = wait.until(drv -> {
                    try {
                        WebElement element = drv.findElement(torneoLocator);
                        if (element.isDisplayed() && element.isEnabled()) {
                            boolean jsVisibleEnabled = (Boolean) ((JavascriptExecutor) drv).executeScript(
                                "return arguments[0].offsetParent !== null && !arguments[0].disabled;", element);
                            if (jsVisibleEnabled) {
                                log.trace("Selector encontrado y listo en este intento.");
                                return element;
                            }
                            log.trace("Selector encontrado pero JS lo ve no listo.");
                            return null;
                        }
                        log.trace("Selector encontrado pero no listo (visible={}, habilitado={})", element.isDisplayed(), element.isEnabled());
                        return null;
                    } catch (NoSuchElementException | StaleElementReferenceException e) {
                        log.trace("Selector aún no presente o stale, reintentando...");
                        return null;
                    }
                });
                log.debug("Selector encontrado, visible y habilitado después de la espera personalizada.");

                // Scroll
                try {
                    log.debug("Forzando scroll hacia el selector...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", selectElem);
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    log.debug("Scroll hacia el selector completado.");
                } catch (Exception scrollEx) {
                    log.warn("No se pudo forzar el scroll hacia el selector: {}", scrollEx.getMessage());
                }

                // Re-obtener referencia fresca
                selectElem = driver.findElement(torneoLocator);

                log.warn("✔ Estado del select ANTES de interactuar: displayed={}, enabled={}", selectElem.isDisplayed(), selectElem.isEnabled());
                log.warn("✔ HTML del select: {}", selectElem.getAttribute("outerHTML"));

                // Intento con Select y Fallback JS
                log.debug("Intentando seleccionar 'LaLiga' con la clase Select...");
                try {
                    new Select(selectElem).selectByVisibleText("LaLiga");
                    log.info("Opción 'LaLiga' seleccionada usando la clase Select.");
                } catch (ElementNotInteractableException | StaleElementReferenceException enie) {
                    log.warn("Select falló ({}). Intentando seleccionar 'LaLiga' con JavaScript como fallback.", enie.getMessage());
                     if (enie instanceof StaleElementReferenceException) {
                         log.debug("Re-localizando elemento stale antes del fallback JS...");
                         try {
                             WebDriverWait shortFindWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                             selectElem = shortFindWait.until(ExpectedConditions.presenceOfElementLocated(torneoLocator));
                         } catch (Exception findEx) { throw new RuntimeException("Fallo crítico al re-localizar selector para fallback JS.", findEx); }
                     }
                     if (selectElem == null) { throw new RuntimeException("Fallo crítico: selectElem es null antes del fallback JS."); }
                    try {
                        String valueToSet = "8"; // ¡¡VERIFICAR ESTE VALOR!!
                        log.warn("Usando JavaScript para establecer value='{}' en el select.", valueToSet);
                        String script = "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('change', { bubbles: true }));";
                        ((JavascriptExecutor) driver).executeScript(script, selectElem, valueToSet);
                        log.info("Opción 'LaLiga' seleccionada (o intentada) usando JavaScript.");
                        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    } catch (Exception jsEx) {
                        log.error("¡FALLÓ también el fallback con JavaScript para seleccionar LaLiga!", jsEx);
                        takeScreenshot(driver, baseScreenshotPath + "_torneo_js_fallback_exception.png");
                        throw new RuntimeException("Fallo crítico al seleccionar LaLiga (ni Select ni JS funcionaron).", jsEx);
                    }
                }
                // --- Fin espera robusta v10 ---

                // --- Espera por la actualización de la tabla (después de seleccionar LaLiga) ---
                log.debug("Esperando que la tabla de jugadores sea VISIBLE después de seleccionar LaLiga...");
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("player-table-statistics-body")));
                log.info("Tabla de jugadores VISIBLE después de seleccionar LaLiga.");
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                log.debug("Pausa post-selección completada.");

            } catch (TimeoutException toe) {
                 log.error("¡ERROR CRÍTICO! Timeout esperando que el selector de torneo estuviera presente y listo: {}. ¿Overlay? ¿Recursos?", toe.getMessage(), toe);
                 takeScreenshot(driver, baseScreenshotPath + "_select_active_wait_timeout_v16.png");
                 throw new RuntimeException("Timeout crítico esperando la disponibilidad del selector de torneo.", toe);
            } catch (Exception e) {
                log.error("¡ERROR CRÍTICO! Fallo inesperado durante la selección de LaLiga: {}", e.getMessage(), e);
                takeScreenshot(driver, baseScreenshotPath + "_laliga_select_unexpected_error.png");
                throw new RuntimeException("Fallo crítico inesperado al procesar selección de LaLiga.", e);
            }


            // Extracción ROBUSTA (Manejo de StaleElement en el bucle - Como en v12)
            log.info("Procediendo a extraer jugadores de la tabla...");
            By tableBodyLocator = By.id("player-table-statistics-body");
            By rowsLocator = By.cssSelector("#player-table-statistics-body tr");
            WebElement tableBody = null;
            try {
                // Esperar presencia de filas directamente (como en v13, simplificado)
                log.debug("Esperando que al menos una fila (tr) esté PRESENTE dentro del tbody...");
                wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(rowsLocator));
                log.info("Al menos una fila (tr) PRESENTE dentro del tbody.");
                tableBody = driver.findElement(tableBodyLocator); // Obtener referencia si las filas existen
            } catch (TimeoutException ex) {
                 log.warn("Timeout esperando la PRESENCIA de filas (tr) dentro del tbody. La tabla podría estar vacía.");
                 takeScreenshot(driver, baseScreenshotPath + "_rows_presence_timeout_v16.png");
                 // Intentar obtener tbody por si solo falló la espera de filas
                 try { tableBody = driver.findElement(tableBodyLocator); } catch (NoSuchElementException ignored) {}
                 if (tableBody != null) { try { log.warn("Contenido HTML del tbody en el timeout de filas: {}", tableBody.getAttribute("innerHTML")); } catch (Exception ignored) {} }
                 // No lanzar excepción, puede que la tabla esté vacía
            }

            List<WebElement> rows = driver.findElements(rowsLocator);
            log.info("Filas encontradas para procesar: {}", rows.size());
            int staleRowCount = 0;

             for (int i = 0; i < rows.size(); i++) {
                WebElement row = rows.get(i);
                try {
                    List<WebElement> cols = row.findElements(By.tagName("td"));
                    if (cols.size() < 15) {
                        log.trace("Fila {} omitida, cols: {}", i, cols.size());
                        continue;
                    }
                    String name;
                    try {
                        WebElement nameSpan = cols.get(0).findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
                        name = nameSpan.getText().trim();
                        if (name.isBlank()) {
                            name = cols.get(0).findElement(By.cssSelector("a.player-link")).getText().trim();
                            log.trace("Fila {}: Nombre fallback link: {}", i, name);
                        } else {
                            log.trace("Fila {}: Nombre span: {}", i, name);
                        }
                    } catch (NoSuchElementException ex) {
                        String cellText = cols.get(0).getText();
                        String[] parts = cellText.split("\\n");
                        name = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                        log.trace("Fila {}: Nombre fallback texto: '{}' de '{}'", i, name, cellText);
                    }
                    if (name.isBlank()) {
                        log.warn("Fila {}: Nombre vacío, omitida. Celda[0]: {}", i, cols.get(0).getText());
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
                    log.warn("⚠️ Fila {} stale. Saltando. Error: {}", i, e.getMessage());
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
                 log.warn("⚠️ La tabla no contenía filas de jugadores.");
            } else if (staleRowCount == rows.size() && !rows.isEmpty()) {
                 log.error("❌ Todas las {} filas se volvieron obsoletas.", rows.size());
                 takeScreenshot(driver, baseScreenshotPath + "_all_rows_stale.png");
            }
            else {
                log.warn("⚠️ No se procesaron jugadores válidos ({} filas, {} stale).", rows.size(), staleRowCount);
                takeScreenshot(driver, baseScreenshotPath + "_no_valid_players_processed.png");
            }

        } catch (TimeoutException e) {
            String waitInfo = (wait != null) ? wait.toString() : "N/A";
            String currentUrl = "N/A";
            if (driver != null) { try { currentUrl = driver.getCurrentUrl(); } catch (Exception urlEx) { currentUrl = "Error al obtener URL: " + urlEx.getMessage(); } }
            // Distinguir timeouts
            if (e.getMessage() != null && (e.getMessage().contains("player-table-statistics-body") || e.getMessage().contains("tr"))) {
                 log.error("El TimeoutException ocurrió esperando la tabla o sus filas (ver logs anteriores).");
            } else if (e.getMessage() != null && e.getMessage().contains("tournamentOptions")) {
                 log.error("El TimeoutException ocurrió esperando el selector de torneo (ver logs anteriores).");
            }
            else {
                 log.error("Timeout general ({}) esperando un elemento. URL actual: {}. Error: {}", waitInfo, currentUrl, e.getMessage(), e);
                 takeScreenshot(driver, baseScreenshotPath + "_general_timeout_error.png");
            }
        } catch (WebDriverException e) {
             if (e.getMessage() != null && e.getMessage().contains("DevToolsActivePort")) {
                 log.error("Error CRÍTICO WebDriver al iniciar Chrome: {}. Causa probable: Recursos insuficientes.", e.getMessage(), e);
             } else if (e.getMessage() != null && e.getMessage().contains("session deleted or not found")) {
                 log.error("Error WebDriver: Sesión cerrada inesperadamente. {}", e.getMessage(), e);
             } else {
                 log.error("Error WebDriver: {}", e.getMessage(), e);
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

} // FIN CLASE ScraperServicePlayers
