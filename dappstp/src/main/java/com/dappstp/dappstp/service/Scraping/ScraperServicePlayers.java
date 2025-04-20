package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.repository.PlayerBarcelonaRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
// ***** Importación necesaria para Select *****
import org.openqa.selenium.support.ui.Select;
// ******************************************

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openqa.selenium.PageLoadStrategy;

@Service
@Slf4j
public class ScraperServicePlayers {

    private final PlayerBarcelonaRepository playerRepository;

    public ScraperServicePlayers(PlayerBarcelonaRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<PlayerBarcelona> scrapeAndSavePlayers() {
        log.info("🚀 Iniciando scraping...");
        List<PlayerBarcelona> players = new ArrayList<>();
        WebDriver driver = null;

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
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
            log.debug("Inicializando ChromeDriver...");
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(180));

            // ***** CORRECCIÓN 1: Definir duración y usarla *****
            Duration waitTimeoutDuration = Duration.ofSeconds(30); // Puedes ajustar este valor si es necesario (ej. 45)
            WebDriverWait wait = new WebDriverWait(driver, waitTimeoutDuration);
            // *************************************************

            // 1. Navegar a la página
            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            log.info("Navegando a {}", url);
            driver.get(url);

            // 2. Cerrar SweetAlert (si aparece)
            try {
                log.debug("Intentando cerrar SweetAlert...");
                By swalClose = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
                WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(swalClose));
                log.debug("SweetAlert encontrado, intentando clic JS.");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.webpush-swal2-shown")));
                log.debug("SweetAlert cerrado.");
            } catch (TimeoutException | NoSuchElementException e) {
                // ***** CORRECCIÓN 2: Usar variable de duración en log *****
                log.debug("SweetAlert no encontrado o no visible en {}s (puede que no haya aparecido).", waitTimeoutDuration.getSeconds());
                // *******************************************************
            } catch (Exception e) {
                log.warn("Excepción inesperada al cerrar SweetAlert: {}", e.getMessage());
            }


            // 3. Cerrar cookies (si aparece)
             try {
                log.debug("Intentando cerrar banner de cookies...");
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                    By.cssSelector("iframe[title='SP Consent Message']")));
                log.debug("Cambiado al iframe de cookies.");
                By accept = By.xpath("//button[contains(., 'Accept') or contains(., 'Aceptar')]");
                WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(accept));
                log.debug("Botón de aceptar cookies encontrado, intentando clic.");
                try { btn.click(); }
                catch (Exception ex) {
                    log.warn("Clic estándar en cookies falló ({}), intentando JS.", ex.getMessage());
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                }
                log.debug("Banner de cookies cerrado.");
                try { Thread.sleep(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            } catch (TimeoutException | NoSuchElementException e) {
                // ***** CORRECCIÓN 3: Usar variable de duración en log *****
                log.debug("Iframe o botón de cookies no encontrado en {}s (puede que no haya aparecido).", waitTimeoutDuration.getSeconds());
                // *******************************************************
            } catch (Exception e) {
                log.warn("Excepción inesperada al cerrar cookies: {}", e.getMessage());
            }
            finally {
                log.debug("Volviendo al contenido principal.");
                driver.switchTo().defaultContent();
            }


            // ***** NUEVO: SELECCIONAR 'LaLiga' EN EL DESPLEGABLE *****
            try {
                log.info("Intentando seleccionar 'LaLiga' en el desplegable de torneos...");
                // Usar el selector CSS basado en el atributo data-backbone...
                By tournamentDropdownSelector = By.cssSelector("select[data-backbone-model-attribute-dd='tournamentOptions']");
                WebElement dropdownElement = wait.until(ExpectedConditions.elementToBeClickable(tournamentDropdownSelector));

                Select tournamentSelect = new Select(dropdownElement);
                // Seleccionar por el texto visible exacto "LaLiga"
                tournamentSelect.selectByVisibleText("LaLiga");

                log.info("'LaLiga' seleccionada. Esperando un poco a que la página se actualice...");
                // Pausa para permitir que el JavaScript de la página recargue la tabla
                try { Thread.sleep(3000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

            } catch (TimeoutException | NoSuchElementException e) {
                log.error("Error: No se pudo encontrar o interactuar con el desplegable de torneos 'LaLiga'. El scraping probablemente fallará.", e);
                // Lanzar excepción porque sin esto, la tabla no será la correcta
                throw new RuntimeException("No se pudo seleccionar el torneo 'LaLiga'", e);
            } catch (Exception e) {
                 log.error("Error inesperado al seleccionar 'LaLiga': {}", e.getMessage(), e);
                 // Lanzar excepción
                 throw new RuntimeException("Error inesperado al seleccionar 'LaLiga'", e);
            }
            // ***** FIN DEL BLOQUE AÑADIDO *****


            // 4. Extraer tabla (esperar a que sea visible)
            log.debug("Esperando la tabla de jugadores (después de seleccionar LaLiga)...");
            // Asegúrate que este ID sigue siendo correcto después de seleccionar LaLiga
            WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("player-table-statistics-body")));
            log.debug("Tabla encontrada. Extrayendo filas...");
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            log.info("Encontradas {} filas en la tabla.", rows.size());

            // Procesar las filas
            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() < 15) { // Verifica si 15 es el número correcto de columnas
                    log.trace("Fila omitida, columnas insuficientes: {}", cols.size());
                    continue;
                }
                String name;
                try {
                    WebElement span = cols.get(0).findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
                    name = span.getText().trim();
                    if (name.isEmpty()) {
                        name = cols.get(0).findElement(By.cssSelector("a.player-link")).getText().trim();
                        log.trace("Nombre obtenido por fallback de link: {}", name);
                    }
                } catch (NoSuchElementException e) {
                    String[] parts = cols.get(0).getText().split("\\n");
                    name = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                    log.trace("Nombre obtenido por fallback de texto directo: {}", name);
                }
                // Verifica que estos índices (4, 6, 7, 14) sean correctos para la tabla de LaLiga
                String matches = cols.get(4).getText().trim();
                int goals    = parseIntSafe(cols.get(6).getText());
                int assists  = parseIntSafe(cols.get(7).getText());
                double rating= parseDoubleSafe(cols.get(14).getText());

                PlayerBarcelona p = new PlayerBarcelona();
                p.setName(name);
                p.setMatches(matches);
                p.setGoals(goals);
                p.setAssists(assists);
                p.setRating(rating);
                players.add(p);
                log.trace("Jugador procesado: {}", name);
            }


            if (!players.isEmpty()) {
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else {
                // Esto podría pasar si la tabla está vacía incluso después de seleccionar LaLiga
                log.warn("⚠️ No se procesaron jugadores de la tabla (¿tabla vacía?).");
            }
        } catch (TimeoutException e) {
             // ***** CORRECCIÓN 4 (Opcional): Usar variable de duración en log *****
             log.error("Timeout esperando un elemento específico (WebDriverWait con {}s): {}", waitTimeoutDuration.getSeconds(), e.getMessage());
             // *****************************************************************
        }
        catch (Exception e) {
            // Captura cualquier otra excepción, incluyendo las RuntimeException de la selección de liga
            log.error("Error general en scraping: ", e);
        } finally {
            if (driver != null) {
                log.info("Cerrando WebDriver.");
                driver.quit();
            }
        }

        log.info("🏁 Scraping finalizado. Jugadores procesados: {}", players.size());
        return players;
    }

    // Métodos helper parseIntSafe y parseDoubleSafe
    private int parseIntSafe(String txt) {
        if (txt==null||txt.isBlank()||txt.equals("-")) return 0;
        try {
            String digitsOnly = txt.split("\\(")[0].replaceAll("[^\\d]", "");
            return digitsOnly.isEmpty() ? 0 : Integer.parseInt(digitsOnly);
        } catch (Exception e) {
            log.warn("Error parseando int: '{}'", txt, e);
            return 0;
        }
    }

    private double parseDoubleSafe(String txt) {
        if (txt==null||txt.isBlank()||txt.equals("-")) return 0.0;
        try {
            String cleaned = txt.replace(",", ".").replaceAll("[^\\d.]", "");
            if (cleaned.isEmpty()) return 0.0;
            int firstDot = cleaned.indexOf('.');
            if (firstDot != -1) {
                int secondDot = cleaned.indexOf('.', firstDot + 1);
                if (secondDot != -1) {
                    cleaned = cleaned.substring(0, secondDot);
                }
            }
             if (cleaned.equals(".")) return 0.0;
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            log.warn("Error parseando double: '{}'", txt, e);
            return 0.0;
        }
    }
}
