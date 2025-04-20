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
// ***** AÑADIR ESTA IMPORTACIÓN *****
import org.openqa.selenium.support.ui.Select;
// ***********************************

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
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30)); // Aumentado a 60s por si acaso

            // 1. Navegar a la página
            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            log.info("Navegando a {}", url);
            driver.get(url);

            // 2. Cerrar SweetAlert (si aparece)
            // (Tu código existente para SweetAlert aquí...)
            try {
                log.debug("Intentando cerrar SweetAlert...");
                By swalClose = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
                // Usar presenceOfElementLocated puede ser más robusto si el botón existe pero tarda en ser visible
                WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(swalClose));
                log.debug("SweetAlert encontrado, intentando clic JS.");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.webpush-swal2-shown")));
                log.debug("SweetAlert cerrado.");
            } catch (TimeoutException | NoSuchElementException e) {
                log.debug("SweetAlert no encontrado o no visible en {}s (puede que no haya aparecido).", wait.getTimeout().getSeconds());
            } catch (Exception e) {
                log.warn("Excepción inesperada al cerrar SweetAlert: {}", e.getMessage());
            }


            // 3. Cerrar cookies (si aparece)
            // (Tu código existente para Cookies aquí...)
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
                try { Thread.sleep(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } // Pequeña pausa opcional
            } catch (TimeoutException | NoSuchElementException e) {
                log.debug("Iframe o botón de cookies no encontrado en {}s (puede que no haya aparecido).", wait.getTimeout().getSeconds());
            } catch (Exception e) {
                log.warn("Excepción inesperada al cerrar cookies: {}", e.getMessage());
            }
            finally {
                log.debug("Volviendo al contenido principal.");
                driver.switchTo().defaultContent(); // Volver siempre al contenido principal
            }


            // ***** NUEVO: SELECCIONAR 'LaLiga' EN EL DESPLEGABLE *****
            try {
                log.info("Intentando seleccionar 'LaLiga' en el desplegable de torneos...");
                By tournamentDropdownSelector = By.cssSelector("select[data-backbone-model-attribute-dd='tournamentOptions']");

                // Esperar a que el desplegable esté presente y sea clickeable
                WebElement dropdownElement = wait.until(ExpectedConditions.elementToBeClickable(tournamentDropdownSelector));

                Select tournamentSelect = new Select(dropdownElement);
                // Seleccionar por el texto visible exacto
                tournamentSelect.selectByVisibleText("LaLiga");

                log.info("'LaLiga' seleccionada. Esperando un poco a que la página se actualice...");
                // Pausa corta para permitir que el JavaScript de la página recargue la tabla
                try { Thread.sleep(3000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } // Aumentado a 3s

            } catch (TimeoutException | NoSuchElementException e) {
                log.error("Error: No se pudo encontrar o interactuar con el desplegable de torneos 'LaLiga'. El scraping probablemente fallará.", e);
                // Considera lanzar una excepción si la selección es crítica
                throw new RuntimeException("No se pudo seleccionar el torneo 'LaLiga'", e);
            } catch (Exception e) {
                 log.error("Error inesperado al seleccionar 'LaLiga': {}", e.getMessage(), e);
                 // También podrías querer relanzar aquí
                 throw new RuntimeException("Error inesperado al seleccionar 'LaLiga'", e);
            }
            // ***** FIN DEL BLOQUE AÑADIDO *****


            // 4. Extraer tabla (esperar a que sea visible)
            log.debug("Esperando la tabla de jugadores (después de seleccionar LaLiga)...");
            // --- ¡VERIFICA ESTE ID DESPUÉS DE SELECCIONAR LALIGA! ---
            WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("player-table-statistics-body")));
            log.debug("Tabla encontrada. Extrayendo filas...");
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            log.info("Encontradas {} filas en la tabla.", rows.size());

            // (Tu código existente para procesar las filas aquí...)
            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() < 15) { // Asegúrate que 15 sigue siendo el número correcto
                    log.trace("Fila omitida, columnas insuficientes: {}", cols.size());
                    continue;
                }

                String name;
                try {
                    // Intenta obtener el nombre del span específico primero
                    WebElement span = cols.get(0).findElement(By.cssSelector("a.player-link span.iconize-icon-left"));
                    name = span.getText().trim();
                    // Fallback si el span está vacío pero el link existe
                    if (name.isEmpty()) {
                        name = cols.get(0).findElement(By.cssSelector("a.player-link")).getText().trim();
                        log.trace("Nombre obtenido por fallback de link: {}", name);
                    }
                } catch (NoSuchElementException e) {
                    // Fallback si no hay link o span, intenta obtener el texto directo
                    String[] parts = cols.get(0).getText().split("\\n");
                    name = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                    log.trace("Nombre obtenido por fallback de texto directo: {}", name);
                }

                // Asegúrate que los índices 4, 6, 7, 14 siguen siendo correctos para la tabla de LaLiga
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
                log.warn("⚠️ No se procesaron jugadores de la tabla.");
            }
        } catch (TimeoutException e) {
             log.error("Timeout esperando un elemento específico (WebDriverWait): {}", e.getMessage());
        }
        catch (Exception e) {
            // Captura errores generales
            log.error("Error general en scraping: ", e); // Loguea la traza completa
        } finally {
            if (driver != null) {
                log.info("Cerrando WebDriver.");
                driver.quit();
            }
        }

        log.info("🏁 Scraping finalizado. Jugadores procesados: {}", players.size());
        return players;
    }

    // (Tus métodos parseIntSafe y parseDoubleSafe aquí...)
    private int parseIntSafe(String txt) {
        if (txt==null||txt.isBlank()||txt.equals("-")) return 0;
        try {
            // Extraer solo los dígitos antes de un posible paréntesis
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
            // Reemplazar coma por punto y quitar caracteres no numéricos excepto el punto
            String cleaned = txt.replace(",", ".").replaceAll("[^\\d.]", "");
            if (cleaned.isEmpty()) return 0.0;
            // Asegurarse de que solo haya un punto decimal (manejar casos como "1.2.3")
            int firstDot = cleaned.indexOf('.');
            if (firstDot != -1) {
                int secondDot = cleaned.indexOf('.', firstDot + 1);
                if (secondDot != -1) {
                    // Si hay un segundo punto, truncar antes de él (o manejar de otra forma si es necesario)
                    cleaned = cleaned.substring(0, secondDot);
                }
            }
            // Evitar NumberFormatException si queda solo "."
             if (cleaned.equals(".")) return 0.0;
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            log.warn("Error parseando double: '{}'", txt, e);
            return 0.0;
        }
    }

}
