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
    // Ruta base para screenshots, accesible desde todos los métodos
    private final String baseScreenshotPath = "/app/screenshot";

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() { // INICIO MÉTODO scrapeAndSavePlayers
        // ACTUALIZACIÓN v10: Usando método robusto seleccionarTorneo con JS check.
        //               ¡¡¡IMPORTANTE!!! RE-VERIFICA shm_size y RAM.
        log.info("🚀 Iniciando scraping de jugadores del Barcelona (v10 - robust select method)...");
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
            log.info("Inicializando ChromeDriver (v10)...");
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
            } catch (Exception e) {
                log.debug("SweetAlert no encontrado o ya cerrado (Timeout corto o error: {}).", e.getMessage());
            }
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
                log.debug("Banner de cookies no encontrado o error (Timeout corto o error: {}).", e.getMessage());
                try { driver.switchTo().defaultContent(); log.debug("Asegurado: Volviendo al contenido principal después de error/no encontrar cookies."); } catch (NoSuchFrameException nfex) { log.trace("Ya estábamos en defaultContent o iframe no existía."); }
            }
            // Pausa explícita DESPUÉS de manejar popups
            if (popupHandled) {
                log.debug("Se manejó al menos un popup. Aplicando pausa de estabilización...");
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                log.debug("Pausa de estabilización completada.");
            } else {
                log.debug("No se detectaron popups para manejar.");
            }
            // --- Fin Manejo de Pop-ups ---


            // --- CAMBIO: Llamar al método refactorizado ---
            seleccionarTorneo(driver, wait);


            // --- Espera por la actualización de la tabla (después de seleccionar LaLiga) ---
            log.debug("Esperando que la tabla de jugadores sea VISIBLE después de seleccionar LaLiga...");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("player-table-statistics-body")));
            log.info("Tabla de jugadores VISIBLE después de seleccionar LaLiga.");
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {} // Pausa post-selección
            log.debug("Pausa post-selección completada.");


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
             // Captura excepciones relanzadas, como las del método seleccionarTorneo
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


    // --- NUEVO MÉTODO REFACTORIZADO ---
    private void seleccionarTorneo(WebDriver driver, WebDriverWait wait) {
        log.info("Intentando seleccionar LaLiga (método robusto v10)...");
        WebElement selectElem = null; // Declarar fuera para usar en logs/fallback
        JavascriptExecutor js = (JavascriptExecutor) driver; // Castear una sola vez

        try {
            By torneoLocator = By.cssSelector("select[data-backbone-model-attribute-dd='tournamentOptions']");

            log.debug("Tomando screenshot ANTES de esperar por el selector (v10)...");
            takeScreenshot(driver, baseScreenshotPath + "_before_select_wait_v10.png");

            log.debug("Esperando que el selector de torneo esté presente...");
            selectElem = wait.until(ExpectedConditions.presenceOfElementLocated(torneoLocator));
            log.debug("Selector presente. Forzando scroll...");

            // Forzar scroll y esperar que esté interactuable
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", selectElem);
            try { Thread.sleep(300); } catch (InterruptedException ignored) {} // Pausa post-scroll

            log.debug("Esperando activamente que el selector esté visible, habilitado y JS-visible...");
            // Espera activa para asegurarse que esté visible y habilitado (Selenium + JS check)
            final WebElement finalSelectElemForLambda = selectElem; // Necesario para usar en lambda
            wait.until(driver1 -> {
                try {
                    // Re-localizar dentro de la lambda es más seguro contra StaleElement
                    WebElement elem = driver1.findElement(torneoLocator);
                    boolean jsVisibleEnabled = (Boolean) js.executeScript(
                        "return arguments[0].offsetParent !== null && !arguments[0].disabled;", elem);
                    boolean seleniumChecks = elem.isDisplayed() && elem.isEnabled();
                    log.trace("Check espera activa: Selenium (disp={}, enab={}), JS (vis/enab={})",
                              seleniumChecks, elem.isDisplayed(), elem.isEnabled(), jsVisibleEnabled);
                    return seleniumChecks && jsVisibleEnabled;
                } catch (NoSuchElementException | StaleElementReferenceException lambdaEx) {
                    log.trace("Check espera activa: Elemento no encontrado o stale, reintentando...");
                    return false; // No está listo, reintentar
                }
            });
            log.debug("Selector confirmado como listo por espera activa.");

            // Re-obtener referencia fresca después de la espera por si acaso
            selectElem = driver.findElement(torneoLocator);

            // Confirmar estado justo antes de interactuar
            log.warn("✔ Estado del select ANTES de interactuar: displayed={}, enabled={}", selectElem.isDisplayed(), selectElem.isEnabled());
            log.warn("✔ HTML del select: {}", selectElem.getAttribute("outerHTML"));

            // Intentar seleccionar con la clase Select
            log.debug("Intentando seleccionar 'LaLiga' con la clase Select...");
            try {
                new Select(selectElem).selectByVisibleText("LaLiga");
                log.info("✅ Opción 'LaLiga' seleccionada usando la clase Select.");

            } catch (ElementNotInteractableException | StaleElementReferenceException enie) {
                // Fallback con JavaScript si Select falla
                log.warn("Select falló ({}). Intentando seleccionar 'LaLiga' con JavaScript como fallback.", enie.getMessage());
                 // Re-localizar si fue Stale
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
                    // Usar JavaScript para cambiar el valor y disparar el evento 'change'
                    // ¡¡VERIFICAR ESTE VALOR!! '8' parecía correcto antes. ChatGPT usó '4' en un ejemplo.
                    String valueToSet = "8";
                    log.warn("Usando JavaScript para establecer value='{}' en el select.", valueToSet);
                    String script = "arguments[0].value = arguments[1]; " +
                                    "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));";
                    js.executeScript(script, selectElem, valueToSet);
                    log.info("✅ Opción 'LaLiga' seleccionada (o intentada) usando JavaScript.");
                    // Pausa más larga después de JS
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

                } catch (Exception jsEx) {
                    log.error("❌ ¡FALLÓ también el fallback con JavaScript para seleccionar LaLiga!", jsEx);
                    takeScreenshot(driver, baseScreenshotPath + "_torneo_js_fallback_exception.png");
                    throw new RuntimeException("Fallo crítico al seleccionar LaLiga (ni Select ni JS funcionaron).", jsEx);
                }
            }

        } catch (TimeoutException e) {
            log.error("❌ Timeout esperando que el selector de torneo estuviera presente y listo para interacción (espera activa)", e);
            takeScreenshot(driver, baseScreenshotPath + "_torneo_active_wait_timeout.png"); // Screenshot específico
            throw new RuntimeException("Timeout: No se pudo encontrar o preparar el selector de torneo.", e);
        } catch (Exception e) {
            log.error("❌ Error general inesperado al intentar seleccionar el torneo", e);
            takeScreenshot(driver, baseScreenshotPath + "_torneo_general_exception.png"); // Screenshot específico
            throw new RuntimeException("Error inesperado durante selección de torneo.", e);
        }
    } // FIN MÉTODO seleccionarTorneo


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

    // Modificado para ser llamado desde seleccionarTorneo también
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
