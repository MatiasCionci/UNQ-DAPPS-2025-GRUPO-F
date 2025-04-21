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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.repository.PlayerBarcelonaRepository;

@Service
public class ScraperServicePlayers {

    private final PlayerBarcelonaRepository playerRepository;

    @Autowired
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
    public List<PlayerBarcelona> scrapeAndSavePlayers() {
        System.out.println("🚀 Iniciando proceso de scraping y guardado...");
        List<PlayerBarcelona> players = new ArrayList<>();

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NONE);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
        // options.addArguments("--headless");
        // options.addArguments("--disable-gpu");

        System.setProperty("webdriver.chrome.driver", "C:\\Users\\cionc\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.manage().window().maximize();
            WebDriverWait generalWait = new WebDriverWait(driver, Duration.ofSeconds(45)); // Aumentado a 45s por si acaso

            By frameLocator = null;
            By cookieButtonSelector = null;
            By sweetAlertPopupSelector = By.cssSelector("div.webpush-swal2-shown");
            By sweetAlertCloseButton = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");

            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            System.out.println("Navegando a: " + url);
            driver.get(url);
            System.out.println("Navegación inicial enviada (NONE). Esperando elementos...");

            try {
                // Una pequeña pausa inicial puede ayudar con PageLoadStrategy.NONE
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // --- MANEJAR POP-UPS ---
            // 3.A. SweetAlert
            System.out.println("Intentando cerrar el pop-up de SweetAlert (webpush)...");
            try {
                System.out.println("Esperando visibilidad del botón de cierre de SweetAlert...");
                // Usar presencia en lugar de visibilidad puede ser más robusto si está oculto inicialmente
                generalWait.until(ExpectedConditions.presenceOfElementLocated(sweetAlertCloseButton));
                WebElement closeButton = driver.findElement(sweetAlertCloseButton);
                System.out.println("Intentando clic con JavaScript en el botón de cierre de SweetAlert...");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeButton);
                generalWait.until(ExpectedConditions.invisibilityOfElementLocated(sweetAlertPopupSelector));
                System.out.println("Pop-up de SweetAlert cerrado.");
                Thread.sleep(1000); // Pausa después de cerrar
            } catch (TimeoutException | NoSuchElementException e) {
                System.out.println("No se encontró o no se pudo cerrar el pop-up de SweetAlert. Continuando...");
            } catch (Exception e) {
                 System.err.println("Error inesperado al intentar cerrar SweetAlert: " + e.getMessage());
            }

            // 3.B. Cookies (iFrame)
            System.out.println("Intentando manejar el banner de cookies (iframe)...");
            try {
                frameLocator = By.cssSelector("iframe[title='SP Consent Message']");
                generalWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameLocator));
                System.out.println("Cambiado al iFrame del consentimiento.");
                // Selector más específico para el botón Aceptar
                cookieButtonSelector = By.xpath("//button[@title='Accept' or @title='Aceptar' or contains(., 'Accept') or contains(., 'Aceptar')]");
                WebElement cookiesBtn = generalWait.until(ExpectedConditions.elementToBeClickable(cookieButtonSelector));
                System.out.println("Botón de cookies encontrado, intentando click...");
                try {
                    cookiesBtn.click();
                } catch (Exception clickException) {
                    System.out.println("Click normal falló, intentando con JavaScript...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cookiesBtn);
                }
                System.out.println("Pop-up de cookies cerrado exitosamente.");
                Thread.sleep(1500); // Pausa después de cerrar
            } catch (TimeoutException | NoSuchElementException e) {
                System.out.println("No se encontró el iFrame o el botón de cookies. Continuando...");
            } catch (Exception e) {
                 System.err.println("Error inesperado al intentar cerrar Cookies: " + e.getMessage());
            }
            finally {
                System.out.println("Volviendo al contenido principal...");
                driver.switchTo().defaultContent(); // Asegurarse de volver siempre
            }

            // ***** NUEVO: SELECCIONAR 'LaLiga' EN EL DESPLEGABLE *****
            try {
                System.out.println("Intentando seleccionar 'LaLiga' en el desplegable de torneos...");
                By tournamentDropdownSelector = By.cssSelector("select[data-backbone-model-attribute-dd='tournamentOptions']");

                // Esperar a que el desplegable esté presente y sea clickeable
                WebElement dropdownElement = generalWait.until(ExpectedConditions.elementToBeClickable(tournamentDropdownSelector));

                Select tournamentSelect = new Select(dropdownElement);
                tournamentSelect.selectByVisibleText("LaLiga");

                System.out.println("'LaLiga' seleccionada. Esperando a que la página se actualice...");
                Thread.sleep(2000); // Aumentar ligeramente la pausa para la actualización de la tabla

            } catch (TimeoutException | NoSuchElementException e) {
                System.err.println("Error: No se pudo encontrar o interactuar con el desplegable de torneos 'LaLiga'.");
                // Considera lanzar una excepción si esto es crítico
                // throw new RuntimeException("No se pudo seleccionar el torneo 'LaLiga'", e);
            } catch (Exception e) {
                 System.err.println("Error inesperado al seleccionar 'LaLiga': " + e.getMessage());
            }
            // ***** FIN DE LA MODIFICACIÓN *****

            // --- EXTRAER DATOS DE LA TABLA ---
            System.out.println("Esperando a que la tabla de estadísticas cargue (después de seleccionar LaLiga)...");
            By tableBodySelector = By.id("player-table-statistics-body");
            // Espera más robusta: esperar a que la tabla sea visible Y contenga al menos una fila
            try {
                 generalWait.until(ExpectedConditions.and(
                     ExpectedConditions.visibilityOfElementLocated(tableBodySelector),
                     ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("#player-table-statistics-body tr"), 0)
                 ));
                 System.out.println("Tabla encontrada y con filas. Extrayendo datos...");
            } catch (TimeoutException e) {
                 System.out.println("Advertencia: La tabla se encontró pero parece estar vacía o no cargó filas a tiempo.");
                 // Intenta encontrar el cuerpo de la tabla de todas formas, puede que la espera fallara por poco
                 try {
                     WebElement tableBody = driver.findElement(tableBodySelector); // No esperar más, solo buscar
                     List<WebElement> rows = tableBody.findElements(By.tagName("tr"));
                     if (rows.isEmpty()) {
                         System.out.println("Confirmado: No se encontraron filas en la tabla después de seleccionar LaLiga.");
                         // Puedes decidir devolver la lista vacía aquí si quieres
                         // return players;
                     }
                 } catch (NoSuchElementException innerEx) {
                     System.err.println("Error crítico: No se encontró el cuerpo de la tabla con ID 'player-table-statistics-body' después de seleccionar LaLiga.");
                     throw innerEx; // Relanzar para que se maneje en el catch principal
                 }
            }

            // Volver a obtener la referencia al cuerpo de la tabla por si acaso
            WebElement tableBody = driver.findElement(tableBodySelector);
            List<WebElement> rows = tableBody.findElements(By.tagName("tr"));
            System.out.println("Número de filas encontradas: " + rows.size());

            if (rows.isEmpty()) {
                 System.out.println("Advertencia: No se encontraron filas en la tabla para procesar.");
            }

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                // Ajusta el número mínimo de columnas si la vista de LaLiga tiene menos/más que antes
                if (cols.size() >= 15) { // Verifica si 15 sigue siendo correcto para LaLiga
                    try {
                        // Extraer datos... (Asegúrate que los índices 0, 4, 6, 7, 14 siguen siendo correctos para LaLiga)
                        WebElement playerInfoCell = cols.get(0);
                        String name = "Nombre no encontrado";
                        // Lógica para extraer nombre (parece robusta, mantenla)
                        try {
                             WebElement nameSpan = playerInfoCell.findElement(By.cssSelector("a.player-link span.iconize.iconize-icon-left"));
                             name = nameSpan.getText().trim();
                             if (name.isEmpty()) { // Fallback si el span está vacío
                                 name = playerInfoCell.findElement(By.cssSelector("a.player-link")).getText().trim();
                             }
                        } catch (NoSuchElementException e) { // Fallback si no hay span
                             String rawText = playerInfoCell.getText();
                             String[] lines = rawText.split("\\n");
                             name = lines.length > 1 ? lines[1].trim() : lines[0].trim(); // Intenta obtener la segunda línea (nombre)
                        }

                        String matches = cols.get(4).getText().trim(); // Índice 4 para Apps
                        int goals = parseIntSafe(cols.get(6).getText()); // Índice 6 para Gls
                        int assists = parseIntSafe(cols.get(7).getText()); // Índice 7 para Asts
                        double rating = parseDoubleSafe(cols.get(14).getText()); // Índice 14 para Rating

                        PlayerBarcelona player = new PlayerBarcelona();
                        player.setName(name);
                        player.setMatches(matches);
                        player.setGoals(goals);
                        player.setAssists(assists);
                        player.setRating(rating);

                        players.add(player);
                        System.out.println("Scraped: " + name + " - " + rating);

                    } catch (IndexOutOfBoundsException iobe) {
                         System.err.println("Error procesando una fila: Índice de columna fuera de límites. ¿Cambió la estructura de la tabla? " + iobe.getMessage());
                    } catch (Exception e) {
                        System.err.println("Error procesando una fila: " + e.getMessage());
                    }
                } else if (!cols.isEmpty()) {
                     System.out.println("Fila omitida por tener solo " + cols.size() + " columnas (se esperaban >= 15).");
                }
            }

            // --- GUARDAR EN BASE DE DATOS ---
            if (!players.isEmpty()) {
                System.out.println("💾 Guardando " + players.size() + " jugadores en la base de datos...");
                // playerRepository.deleteAll(); // Descomenta si quieres borrar antes de guardar
                // System.out.println("🧹 Datos anteriores borrados.");
                playerRepository.saveAll(players);
                System.out.println("✅ Jugadores guardados exitosamente.");
            } else {
                System.out.println("⚠️ No se encontraron jugadores válidos para guardar después del scraping.");
            }

            System.out.println("🏁 Scraping y guardado completados.");

        } catch (TimeoutException e) {
            System.err.println("Error: Timeout esperando un elemento: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Ocurrió un error inesperado durante el scraping/guardado:");
            e.printStackTrace();
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
                System.out.println("Cerrando el navegador...");
                driver.quit();
            }
        }
        System.out.println("Proceso finalizado. Total jugadores procesados en esta ejecución: " + players.size());
        return players;
    } // FIN MÉTODO scrapeAndSavePlayers


    // --- Métodos parseIntSafe y parseDoubleSafe (sin cambios) ---
    private static int parseIntSafe(String text) {
        if (text == null || text.trim().isEmpty() || text.trim().equals("-")) { return 0; }
        try {
            String cleanedText = text.replaceAll("[^\\d-]", "");
            if (cleanedText.isEmpty() || cleanedText.equals("-")) return 0;
            return Integer.parseInt(cleanedText);
        } catch (NumberFormatException e) { return 0; }
    }

    private static double parseDoubleSafe(String text) {
        if (text == null || text.trim().isEmpty()) { return 0.0; }
        try {
            // Limpiar y reemplazar coma por punto, manejar guion como 0.0
            String cleanedText = text.trim().replace(",", ".").replace("-", "0.0");
            // Si después de limpiar queda vacío (ej. era solo "-"), devolver 0.0
            if (cleanedText.isEmpty()) return 0.0;
            return Double.parseDouble(cleanedText);
        } catch (NumberFormatException e) { return 0.0; }
    }

} // FIN CLASE ScraperServicePlayers
