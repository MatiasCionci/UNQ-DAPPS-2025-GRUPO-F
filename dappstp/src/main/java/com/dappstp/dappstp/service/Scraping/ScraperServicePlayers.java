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

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() { // INICIO MÉTODO scrapeAndSavePlayers
        // ACTUALIZACIÓN v18.4: Intento SweetAlert (6s max), sin cookies, con selección de Liga.
        log.info("🚀 Iniciando scraping de jugadores del Barcelona (v18.4 - SweetAlert(6s), no cookies, select Liga)...");
        List<PlayerBarcelona> players = new ArrayList<>();
        WebDriver driver = null;
        WebDriverWait wait = null; // Wait principal (largo)
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
        );

        try {
            log.info("Inicializando ChromeDriver (v18.4)...");
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

            // Wait principal para operaciones largas (navegación, carga tabla post-selección)
            wait = new WebDriverWait(driver, Duration.ofSeconds(180)); // 3 minutos

            log.info("Navegando a la página...");
            driver.get("https://www.whoscored.com/teams/65/show/spain-barcelona");
            log.info("Página solicitada (PageLoadStrategy EAGER).");

            // Esperar explícitamente a document.readyState === 'complete'
            try {
                // Usar un wait más corto para readyState, ya que EAGER debería ser rápido
                WebDriverWait readyWait = new WebDriverWait(driver, Duration.ofSeconds(120));
                log.debug("Esperando document.readyState === 'complete' (max 60s)...");
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
                // Wait específico y corto para el SweetAlert
                WebDriverWait sweetAlertWait = new WebDriverWait(driver, Duration.ofSeconds(6)); // <<<--- 6 SEGUNDOS
                log.debug("Buscando SweetAlert (max 6s)...");
                By swalClose = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
                WebElement btn = sweetAlertWait.until(ExpectedConditions.visibilityOfElementLocated(swalClose));
                log.debug("SweetAlert encontrado, intentando cerrar...");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                // Esperar brevemente a que desaparezca (usando el mismo wait corto)
                sweetAlertWait.until(ExpectedConditions.invisibilityOfElementLocated(swalClose));
                log.info("SweetAlert cerrado con éxito.");
                sweetAlertClosed = true;
            } catch (TimeoutException e) {
                log.info("SweetAlert no encontrado o no cerrado en 6 segundos. Continuando...");
            } catch (Exception e) {
                // Capturar otros posibles errores (NoSuchElement, etc.) sin detener el script
                log.warn("Error inesperado al intentar cerrar SweetAlert (continuando): {}", e.getMessage());
            }

            // Banner de Cookies: ELIMINADO
            log.info("Manejo del banner de cookies OMITIDO (v18.4).");

            // Pausa opcional si el SweetAlert FUE cerrado (puede ayudar a estabilizar)
            if (sweetAlertClosed) {
                log.debug("Aplicando pausa de estabilización post-cierre SweetAlert...");
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {} // Pausa corta
                log.debug("Pausa de estabilización completada.");
            }
            // --- Fin Manejo de Pop-ups ---


            // --- CAMBIO: Selección de Liga REINTRODUCIDA ---
            // !!! VERIFICAR SELECTORES: #stages y linkText("LaLiga") podrían haber cambiado !!!
            try {
                log.info("Intentando seleccionar 'LaLiga'...");
                WebDriverWait selectWait = new WebDriverWait(driver, Duration.ofSeconds(60)); // Wait para elementos de selección

                // 1. Encontrar y hacer clic en el desplegable (si es necesario)
                // Asumiendo que es un div/span que abre un menú, no un <select> real
                 By tournamentSelectorTrigger = By.cssSelector("a.tournament-link"); // EJEMPLO - AJUSTAR SELECTOR
                 // O si es un <select>: By tournamentSelectorTrigger = By.id("stages");

                log.debug("Esperando visibilidad del selector de torneo...");
                WebElement selectorTrigger = selectWait.until(ExpectedConditions.visibilityOfElementLocated(tournamentSelectorTrigger));

                // Scroll hacia el selector si es necesario
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", selectorTrigger);
                    Thread.sleep(300);
                } catch(Exception scrollEx) {log.warn("Scroll al selector falló: {}", scrollEx.getMessage());}


                // Si NO es un <select>, hacer clic para abrir el menú
                // Si ES un <select>, comentar esta línea y usar Select más abajo
                log.debug("Haciendo clic en el trigger del selector...");
                selectWait.until(ExpectedConditions.elementToBeClickable(selectorTrigger)).click();
                log.debug("Trigger del selector clickeado.");
                Thread.sleep(500); // Pequeña pausa para que aparezca el menú

                // 2. Encontrar y hacer clic en la opción "LaLiga"
                // Asumiendo que es un enlace <a> dentro del menú desplegado
                By laLigaOption = By.xpath("//ul[contains(@class, 'tournament-list')]//a[contains(text(), 'LaLiga')]"); // EJEMPLO - AJUSTAR SELECTOR
                log.debug("Esperando y haciendo clic en la opción 'LaLiga'...");
                WebElement laLigaLink = selectWait.until(ExpectedConditions.elementToBeClickable(laLigaOption));
                laLigaLink.click();
                log.info("✅ Opción 'LaLiga' seleccionada.");

                // 3. Esperar a que la tabla se actualice (IMPORTANTE)
                //    Una forma es esperar a que el tbody se vuelva "stale" (obsoleto) y luego reaparezca,
                //    o esperar a que un indicador de carga desaparezca.
                //    Aquí usamos una pausa simple + espera de presencia como fallback.
                log.debug("Esperando posible recarga de tabla post-selección de liga...");
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {} // Pausa para permitir inicio de recarga
                // Re-esperar la presencia del tbody después de la selección
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("player-table-statistics-body")));
                log.debug("Tabla (tbody) presente después de seleccionar liga.");


                // --- Alternativa si fuera un <select> estándar ---
                /*
                log.debug("Esperando que el <select> de torneo sea clickeable...");
                WebElement selectElement = selectWait.until(ExpectedConditions.elementToBeClickable(By.id("stages"))); // Asumiendo ID="stages"
                Select tournamentSelect = new Select(selectElement);
                log.debug("Seleccionando 'LaLiga' por texto visible...");
                tournamentSelect.selectByVisibleText("LaLiga"); // O usar selectByValue si tiene un valor específico
                log.info("✅ Opción 'LaLiga' seleccionada desde <select>.");
                // Esperar recarga tabla... (como arriba)
                log.debug("Esperando posible recarga de tabla post-selección de liga...");
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("player-table-statistics-body")));
                log.debug("Tabla (tbody) presente después de seleccionar liga.");
                */
                // --- Fin Alternativa <select> ---


            } catch (TimeoutException | NoSuchElementException e) {
                log.error("❌ Error CRÍTICO al intentar seleccionar 'LaLiga'. ¿Cambiaron los selectores? {}", e.getMessage(), e);
                takeScreenshot(driver, baseScreenshotPath + "_league_select_error_v18.4.png");
                // Detener el proceso si no se puede seleccionar la liga correcta
                throw new RuntimeException("Fallo al seleccionar LaLiga, no se puede continuar.", e);
            } catch (Exception e) {
                 log.error("❌ Error inesperado durante la selección de 'LaLiga': {}", e.getMessage(), e);
                 takeScreenshot(driver, baseScreenshotPath + "_league_select_unexpected_error_v18.4.png");
                 throw new RuntimeException("Error inesperado seleccionando LaLiga.", e);
            }
            // --- FIN Selección de Liga ---


            // --- Espera Tabla (Post-Selección Liga) ---
            By tableBodyLocator = By.id("player-table-statistics-body");
            By rowsLocator = By.cssSelector("#player-table-statistics-body tr");
            WebElement tableBody = null;

            try {
                // Usar el wait principal (largo) aquí
                log.debug("Esperando que el contenedor de la tabla (tbody) esté PRESENTE post-selección...");
                tableBody = wait.until(ExpectedConditions.presenceOfElementLocated(tableBodyLocator));
                log.info("Contenedor de tabla (tbody) PRESENTE post-selección.");

                // Scroll opcional (puede ser necesario de nuevo)
                try {
                    log.debug("Forzando scroll hacia la tabla (post-selección)...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", tableBody);
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    log.debug("Scroll hacia la tabla completado (post-selección).");
                } catch (Exception scrollEx) {
                    log.warn("No se pudo forzar el scroll hacia la tabla (post-selección): {}", scrollEx.getMessage());
                }

                log.debug("Esperando que al menos una fila (tr) esté PRESENTE dentro del tbody (post-selección)...");
                wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(rowsLocator, 0));
                log.info("Al menos una fila (tr) PRESENTE dentro del tbody (post-selección).");

            } catch (TimeoutException e) {
                 log.error("¡ERROR CRÍTICO! Timeout esperando la PRESENCIA del tbody o filas (post-selección): {}", e.getMessage(), e);
                 takeScreenshot(driver, baseScreenshotPath + "_tbody_or_rows_presence_timeout_post_select_v18.4.png");
                 try {
                     String bodyHtml = driver.findElement(By.tagName("body")).getAttribute("outerHTML");
                     log.error("HTML del body en el momento del timeout (post-selección):\n{}", bodyHtml.substring(0, Math.min(bodyHtml.length(), 5000)));
                 } catch (Exception htmlEx) { log.error("No se pudo obtener el HTML del body."); }
                 throw new RuntimeException("Timeout crítico esperando la tabla/filas después de seleccionar liga.", e);
            } catch (NoSuchElementException nse) {
                 log.error("Error crítico: No se encontró #player-table-statistics-body (post-selección).", nse);
                 takeScreenshot(driver, baseScreenshotPath + "_initial_tbody_not_found_presence_post_select_v18.4.png");
                 throw new RuntimeException("No se pudo encontrar el tbody después de seleccionar liga.", nse);
            }

            // Extracción ROBUSTA (Sin cambios en la lógica interna del bucle)
            log.info("Procediendo a extraer jugadores de la tabla (post-selección)...");
            List<WebElement> rows = driver.findElements(rowsLocator); // Re-buscar filas por si la tabla cambió
            log.info("Filas encontradas para procesar (post-selección): {}", rows.size());
            int staleRowCount = 0;

             for (int i = 0; i < rows.size(); i++) {
                WebElement row = rows.get(i);
                try {
                    List<WebElement> cols = row.findElements(By.tagName("td"));
                    if (cols.isEmpty() || cols.size() < 15) {
                        log.trace("Fila {} omitida, cols: {} (insuficientes o vacías)", i, cols.size());
                        continue;
                    }
                    // ... (resto de la lógica de extracción de datos de la fila sin cambios) ...
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
                 log.warn("⚠️ La tabla (post-selección) no contenía filas de jugadores.");
                 takeScreenshot(driver, baseScreenshotPath + "_no_rows_found_post_select_v18.4.png");
            } else if (!rows.isEmpty() && staleRowCount == rows.size()) {
                 log.error("❌ Todas las {} filas (post-selección) se volvieron obsoletas.", rows.size());
                 takeScreenshot(driver, baseScreenshotPath + "_all_rows_stale_post_select_v18.4.png");
            }
            else {
                log.warn("⚠️ No se procesaron jugadores válidos ({} filas post-selección, {} stale).", rows.size(), staleRowCount);
                takeScreenshot(driver, baseScreenshotPath + "_no_valid_players_processed_post_select_v18.4.png");
            }

        } catch (TimeoutException e) { // (Manejo de excepciones sin cambios significativos)
            String waitInfo = (wait != null) ? wait.toString() : "N/A";
            String currentUrl = "N/A";
            String readyState = "N/A";
            if (driver != null) {
                try { currentUrl = driver.getCurrentUrl(); } catch (Exception urlEx) { currentUrl = "Error URL: " + urlEx.getMessage(); }
                try { readyState = (String)((JavascriptExecutor) driver).executeScript("return document.readyState"); } catch (Exception rsEx) { readyState = "Error State: " + rsEx.getMessage(); }
            }
            // Diferenciar si el timeout fue esperando la liga o la tabla final
            if (e.getMessage() != null && e.getMessage().contains("LaLiga")) { // Asumiendo que el mensaje de error contendría "LaLiga"
                 log.error("Timeout esperando elementos para seleccionar LaLiga. URL: {}, Estado: {}", currentUrl, readyState, e);
            } else if (e.getMessage() != null && (e.getMessage().contains("document.readyState") )) {
                 log.error("Timeout esperando document.readyState. URL: {}, Estado Actual: {}", currentUrl, readyState, e);
            } else if (e.getMessage() != null && (e.getMessage().contains("player-table-statistics-body") || e.getMessage().contains("tr"))) {
                 log.error("Timeout esperando la tabla o sus filas (post-selección). URL: {}, Estado: {}", currentUrl, readyState, e);
            } else {
                 log.error("Timeout general ({}) esperando un elemento. URL: {}, Estado: {}. Error: {}", waitInfo, currentUrl, readyState, e.getMessage(), e);
            }
            // La captura de pantalla se toma según el nombre de archivo definido en el bloque catch específico si existe
            // takeScreenshot(driver, baseScreenshotPath + "_general_timeout_error_v18.4.png"); // Puede ser redundante si ya se tomó una más específica
        } catch (WebDriverException e) { // (Manejo de excepciones sin cambios)
             if (e.getMessage() != null && e.getMessage().contains("DevToolsActivePort")) {
                 log.error("Error CRÍTICO WebDriver al iniciar Chrome: {}. Causa probable: Recursos insuficientes (RAM, /dev/shm).", e.getMessage(), e);
             } else if (e.getMessage() != null && e.getMessage().contains("session deleted or not found")) {
                 log.error("Error WebDriver: Sesión cerrada inesperadamente. {}", e.getMessage(), e);
             } else {
                 log.error("Error WebDriver: {}", e.getMessage(), e);
             }
             takeScreenshot(driver, baseScreenshotPath + "_webdriver_error_v18.4.png");
        } catch (RuntimeException e) { // Captura la excepción relanzada (ej. fallo selección liga)
             log.error("Scraping detenido debido a error previo: {}", e.getMessage());
             // La captura de pantalla debería haberse tomado en el bloque catch original (ej. selección liga)
        } catch (Exception e) { // (Manejo de excepciones sin cambios)
            log.error("Error general inesperado en scraping: {}", e.getMessage(), e);
            takeScreenshot(driver, baseScreenshotPath + "_unexpected_error_v18.4.png");
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

        log.info("🏁 Scraping finalizado (v18.4). Total procesados: {} jugadores.", players.size());
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
