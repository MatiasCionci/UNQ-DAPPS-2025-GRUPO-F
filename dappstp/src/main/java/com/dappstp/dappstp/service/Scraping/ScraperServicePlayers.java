package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.repository.PlayerBarcelonaRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
// Lombok opcional para inyección de dependencias más limpia
// import lombok.RequiredArgsConstructor;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select; // Asegúrate de tener esta importación
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream; // Para guardar screenshot
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openqa.selenium.PageLoadStrategy;

@Service
@Slf4j
// @RequiredArgsConstructor // Descomenta si prefieres inyección por constructor con Lombok
public class ScraperServicePlayers {

    // Si usas @RequiredArgsConstructor, hazlo final:
    // private final PlayerBarcelonaRepository playerRepository;
    private final PlayerBarcelonaRepository playerRepository;

    // Si NO usas @RequiredArgsConstructor, necesitas un constructor:
    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
       this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() {
        log.info("🚀 Iniciando scraping combinado de jugadores del Barcelona...");
        List<PlayerBarcelona> players = new ArrayList<>();
        WebDriver driver = null;
        String screenshotPath = "/app/screenshot_error_scraping.png"; // Ruta para Render

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER); // Estrategia EAGER
        // --- Opciones Esenciales para Headless/Docker/Render ---
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
            + " AppleWebKit/537.36 (KHTML, like Gecko)"
            + " Chrome/135.0.7049.95 Safari/537.36");
        String userDataDir = "/tmp/chrome-profile-" + UUID.randomUUID();
        options.addArguments("--user-data-dir=" + userDataDir);

        try {
            log.debug("Inicializando ChromeDriver con opciones...");
            driver = new ChromeDriver(options);

            // Timeout para carga de página (menos relevante con EAGER, pero como fallback)
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(180));

            // Timeout para esperas explícitas (WebDriverWait) - Aumentado a 60s
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

            // 1. Navegar a la página
            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            log.info("Navegando a {}", url);
            driver.get(url);

            // 2. Cerrar SweetAlert (si aparece)
            try {
                log.debug("Intentando cerrar SweetAlert (si existe)...");
                By swalClose = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
                wait.until(ExpectedConditions.visibilityOfElementLocated(swalClose));
                WebElement btn = driver.findElement(swalClose);
                log.debug("SweetAlert encontrado, intentando clic JS.");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.webpush-swal2-shown")));
                log.info("SweetAlert cerrado.");
            } catch (TimeoutException | NoSuchElementException e) {
                log.debug("SweetAlert no encontrado o no visible en {}s.", wait.toString());
            } catch (Exception e) {
                log.warn("Excepción inesperada al intentar cerrar SweetAlert: {}", e.getMessage());
            }

            // 3. Cerrar cookies (si aparece)
            try {
                log.debug("Intentando cerrar banner de cookies (si existe)...");
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                    By.cssSelector("iframe[title='SP Consent Message']")));
                log.debug("Cambiado al iframe de cookies.");
                By acceptBtn = By.xpath("//button[contains(., 'Accept') or contains(., 'Aceptar')]");
                WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(acceptBtn));
                log.debug("Botón de aceptar cookies encontrado, intentando clic.");
                try {
                    btn.click();
                } catch (ElementClickInterceptedException ex) {
                    log.warn("Clic estándar en cookies interceptado ({}), intentando JS.", ex.getMessage());
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                }
                log.info("Banner de cookies cerrado.");
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            } catch (TimeoutException | NoSuchElementException e) {
                log.debug("Iframe o botón de cookies no encontrado en {}s.", wait.toString());
            } catch (Exception e) {
                log.warn("Excepción inesperada al intentar cerrar cookies: {}", e.getMessage());
            } finally {
                log.debug("Volviendo al contenido principal desde el iframe (o si no se entró).");
                driver.switchTo().defaultContent();
            }

            // 4. Esperar y seleccionar "LaLiga" en el dropdown (Punto Crítico)
            By torneoLocator = By.cssSelector("select[data-backbone-model-attribute-dd='tournamentOptions']");
            try {
                log.debug("Esperando que el selector de torneo '{}' esté presente...", torneoLocator);
                WebElement torneoElement = wait.until(ExpectedConditions.presenceOfElementLocated(torneoLocator));
                log.debug("Selector presente. Esperando que sea clickeable...");
                wait.until(ExpectedConditions.elementToBeClickable(torneoLocator));
                log.debug("Selector listo para interactuar.");

                log.debug("Obteniendo referencia a la tabla actual antes de la selección...");
                WebElement oldTableBody = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.id("player-table-statistics-body")
                ));

                Select torneoSelect = new Select(torneoElement);
                log.info("Seleccionando 'LaLiga' en el dropdown...");
                torneoSelect.selectByVisibleText("LaLiga");

                log.debug("Esperando que la tabla de jugadores se actualice (staleness)...");
                wait.until(ExpectedConditions.stalenessOf(oldTableBody));
                log.debug("Tabla anterior 'stale'. Esperando la visibilidad de la nueva tabla...");
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("player-table-statistics-body")));
                log.info("Tabla de jugadores actualizada para LaLiga.");

            } catch (TimeoutException | NoSuchElementException e) {
                 // Error crítico: No se pudo seleccionar LaLiga
                 log.error("¡ERROR CRÍTICO! No se pudo encontrar o interactuar con el selector de LaLiga ('{}') en {}s. ¿Cambió la página?",
                          torneoLocator, wait.toString(), e);
                 takeScreenshot(driver, screenshotPath + "_laliga_error.png"); // Tomar screenshot
                 throw new RuntimeException("Fallo al seleccionar LaLiga, abortando scraping.", e); // Relanzar para detener
            } catch (Exception e) {
                log.error("Error inesperado al seleccionar LaLiga: {}", e.getMessage(), e);
                takeScreenshot(driver, screenshotPath + "_laliga_unexpected_error.png");
                throw new RuntimeException("Fallo inesperado al seleccionar LaLiga.", e); // Relanzar
            }

            // 5. Extraer tabla de jugadores (después de seleccionar LaLiga)
            log.debug("Procediendo a extraer datos de la tabla de jugadores...");
            WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("player-table-statistics-body")));
            log.debug("Tabla encontrada. Extrayendo filas...");
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            log.info("Encontradas {} filas en la tabla.", rows.size());

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() < 15) { // Verificar que tengamos suficientes columnas para los datos
                    log.trace("Fila omitida, columnas insuficientes: {}", cols.size());
                    continue;
                }

                // Extracción de Nombre (manejando múltiples líneas)
                String name;
                try {
                    // Intenta obtener el nombre del span dentro del enlace (más específico)
                    WebElement nameSpan = cols.get(0)
                        .findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
                    name = nameSpan.getText().trim();
                    if (name.isEmpty()) { // Fallback si el span está vacío
                        name = cols.get(0).findElement(By.cssSelector("a.player-link")).getText().trim();
                        log.trace("Nombre obtenido por fallback de link (span vacío): {}", name);
                    } else {
                         log.trace("Nombre obtenido del span: {}", name);
                    }
                } catch (NoSuchElementException e) {
                    // Fallback si no hay enlace/span: usar split en el texto de la celda
                    String cellText = cols.get(0).getText();
                    String[] parts = cellText.split("\\n");
                    name = parts.length > 1 ? parts[1].trim() : parts[0].trim(); // Toma la segunda línea si existe
                    log.trace("Nombre obtenido por fallback de texto directo (split): '{}' de '{}'", name, cellText);
                }

                if (name.isBlank()) {
                    log.warn("Nombre vacío detectado en la fila, omitiendo jugador. Contenido celda[0]: {}", cols.get(0).getText());
                    continue;
                }

                // Extracción de otros datos
                String matches = cols.get(4).getText().trim(); // Columna 5 -> índice 4
                int goals    = parseIntSafe(cols.get(6).getText()); // Columna 7 -> índice 6
                int assists  = parseIntSafe(cols.get(7).getText()); // Columna 8 -> índice 7
                double rating= parseDoubleSafe(cols.get(14).getText()); // Columna 15 -> índice 14

                PlayerBarcelona p = new PlayerBarcelona();
                p.setName(name);
                p.setMatches(matches); // Asumiendo que Matches es String en tu modelo
                p.setGoals(goals);
                p.setAssists(assists);
                p.setRating(rating);
                players.add(p);
                log.trace("Jugador procesado: Nombre='{}', Partidos='{}', Goles={}, Asist={}, Rating={}", name, matches, goals, assists, rating);
            }

            if (!players.isEmpty()) {
                log.info("Guardando {} jugadores en la base de datos...", players.size());
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados exitosamente.", players.size());
            } else {
                log.warn("⚠️ No se procesaron jugadores válidos de la tabla después de seleccionar LaLiga.");
            }
        } catch (TimeoutException e) {
             log.error("Timeout general de WebDriverWait ({}) esperando un elemento: {}", wait.toString(), e.getMessage(), e);
             takeScreenshot(driver, screenshotPath + "_timeout_error.png");
        } catch (WebDriverException e) {
             log.error("Error de WebDriver durante el scraping: {}", e.getMessage(), e);
             // Puede que no se pueda tomar screenshot si el driver murió
             takeScreenshot(driver, screenshotPath + "_webdriver_error.png");
        } catch (RuntimeException e) {
             // Captura las excepciones relanzadas desde el bloque de LaLiga
             log.error("Scraping detenido debido a error previo: {}", e.getMessage());
             // El screenshot ya se debería haber tomado en el catch original
        }
         catch (Exception e) {
            log.error("Error general inesperado durante el proceso de scraping: ", e);
            takeScreenshot(driver, screenshotPath + "_general_error.png");
        } finally {
            if (driver != null) {
                log.info("Cerrando WebDriver...");
                try {
                    driver.quit();
                    log.info("WebDriver cerrado correctamente.");
                } catch (Exception e) {
                    log.error("Error al cerrar WebDriver: {}", e.getMessage());
                }
            }
        }

        log.info("🏁 Scraping finalizado. Total jugadores procesados: {}", players.size());
        return players;
    }

    // --- Métodos de parseo seguro ---
    private int parseIntSafe(String txt) {
        if (txt == null || txt.isBlank() || txt.equals("-")) return 0;
        try {
            String digitsOnly = txt.split("\\(")[0].replaceAll("[^\\d]", "");
            return digitsOnly.isEmpty() ? 0 : Integer.parseInt(digitsOnly);
        } catch (NumberFormatException e) {
            log.warn("Error parseando int desde '{}': {}", txt, e.getMessage());
            return 0;
        } catch (Exception e) {
            log.warn("Error inesperado parseando int desde '{}': {}", txt, e.getMessage());
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
                if (secondDot != -1) {
                    cleaned = cleaned.substring(0, secondDot);
                }
            }
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Error parseando double desde '{}': {}", txt, e.getMessage());
            return 0.0;
        } catch (Exception e) {
            log.warn("Error inesperado parseando double desde '{}': {}", txt, e.getMessage());
            return 0.0;
        }
    }

    // --- Método para tomar Screenshot ---
    private void takeScreenshot(WebDriver driver, String filePath) {
        if (driver instanceof TakesScreenshot) {
            try {
                byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(screenshotBytes);
                    log.warn("📸 Captura de pantalla guardada en: {}", filePath);
                }
            } catch (Exception e) {
                log.error("No se pudo guardar la captura de pantalla en '{}': {}", filePath, e.getMessage());
            }
        } else {
            log.warn("El WebDriver actual no soporta la toma de capturas de pantalla.");
        }
    }
}
