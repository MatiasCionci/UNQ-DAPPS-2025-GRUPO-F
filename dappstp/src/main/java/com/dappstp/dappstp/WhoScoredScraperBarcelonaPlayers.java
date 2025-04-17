package com.dappstp.dappstp;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WhoScoredScraperBarcelonaPlayers {

    public static void main(String[] args) {

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NONE);
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"); // Actualiza si es necesario

        System.setProperty("webdriver.chrome.driver", "C:\\Users\\cionc\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        WebDriverWait generalWait = new WebDriverWait(driver, Duration.ofSeconds(45));

        By frameLocator = null;
        By cookieButtonSelector = null;
        By sweetAlertPopupSelector = By.cssSelector("div.webpush-swal2-shown");
        // *** VERIFICA ESTE SELECTOR CUIDADOSAMENTE ***
        By sweetAlertCloseButton = By.cssSelector("div.webpush-swal2-shown button.webpush-swal2-close");
        // Alternativa por si tiene un aria-label específico:
        // By sweetAlertCloseButton = By.cssSelector("div.webpush-swal2-shown button[aria-label='Close this dialog']");

        try {
            String url = "https://www.whoscored.com/teams/65/show/spain-barcelona";
            System.out.println("Navegando a: " + url);
            driver.get(url);
            System.out.println("Navegación inicial enviada (NONE). Esperando elementos...");

            // Pequeña pausa para que los scripts iniciales arranquen
            try {
                Thread.sleep(2000); // 2 segundos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }


            // --- 3. MANEJAR POP-UPS (SweetAlert primero, luego Cookies) ---

            // 3.A. Intentar cerrar el pop-up de SweetAlert (webpush) - ENFOQUE MODIFICADO
            System.out.println("Intentando cerrar el pop-up de SweetAlert (webpush)...");
            try {
                // 1. Esperar solo a que el botón sea VISIBLE
                System.out.println("Esperando visibilidad del botón de cierre de SweetAlert...");
                generalWait.until(ExpectedConditions.visibilityOfElementLocated(sweetAlertCloseButton));
                System.out.println("Botón de cierre de SweetAlert visible.");

                // 2. Encontrar el elemento
                WebElement closeButton = driver.findElement(sweetAlertCloseButton);

                // 3. Forzar clic con JavaScript
                System.out.println("Intentando clic con JavaScript en el botón de cierre de SweetAlert...");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeButton);

                System.out.println("Clic JS enviado a SweetAlert. Esperando que desaparezca...");

                // 4. Esperar a que desaparezca visualmente
                generalWait.until(ExpectedConditions.invisibilityOfElementLocated(sweetAlertPopupSelector));
                System.out.println("Pop-up de SweetAlert cerrado.");
                Thread.sleep(1000); // Pequeña pausa adicional

            } catch (TimeoutException | NoSuchElementException e) {
                System.out.println("No se encontró o no se pudo cerrar el pop-up de SweetAlert (puede que no haya aparecido o el selector sea incorrecto). Continuando...");
                // Añadir más detalles al error
                System.err.println("Detalle (SweetAlert): " + e.getMessage());
                if (sweetAlertCloseButton != null) {
                    System.err.println("Selector usado para SweetAlert: " + sweetAlertCloseButton.toString());
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.err.println("Espera interrumpida al cerrar SweetAlert.");
            } catch (Exception e) {
                 System.err.println("Error inesperado al intentar cerrar SweetAlert: " + e.getMessage());
                 e.printStackTrace();
            }


            // 3.B. Intentar cerrar el banner de Cookies (dentro del iFrame) - Sin cambios en la lógica interna
            System.out.println("Intentando manejar el banner de cookies (iframe)...");
            try {
                frameLocator = By.cssSelector("iframe[title='SP Consent Message']"); // *** ¡VERIFICA ESTE SELECTOR! ***
                generalWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameLocator));
                System.out.println("Cambiado al iFrame del consentimiento.");

                System.out.println("Buscando el botón de aceptar cookies dentro del iFrame...");
                cookieButtonSelector = By.xpath("//button[contains(., 'Accept') or contains(., 'Aceptar')]"); // *** ¡VERIFICA ESTE SELECTOR! ***
                WebElement cookiesBtn = generalWait.until(ExpectedConditions.elementToBeClickable(cookieButtonSelector)); // Mantenemos clickable aquí, suele ser más fiable para este

                System.out.println("Intentando clic estándar en el botón de cookies...");
                try {
                    cookiesBtn.click();
                } catch (Exception clickException) {
                    System.err.println("Clic estándar en cookies falló, intentando con JavaScript: " + clickException.getMessage());
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cookiesBtn);
                }
                System.out.println("Pop-up de cookies cerrado exitosamente.");
                Thread.sleep(1500);

            } catch (TimeoutException | NoSuchElementException e) {
                System.out.println("No se encontró el iFrame o el botón de cookies dentro del tiempo esperado. Continuando...");
                String exceptionMessage = e.getMessage();
                if (exceptionMessage != null) {
                     if (cookieButtonSelector != null && exceptionMessage.contains(cookieButtonSelector.toString())) {
                         System.err.println("Detalle: Falló esperando el botón de cookies con selector: " + cookieButtonSelector.toString());
                     } else if (frameLocator != null && exceptionMessage.contains(frameLocator.toString())) {
                         System.err.println("Detalle: Falló esperando el iframe de cookies con selector: " + frameLocator.toString());
                     } else {
                         System.err.println("Detalle (Cookies): Timeout o elemento no encontrado. Mensaje: " + exceptionMessage);
                     }
                } else {
                     System.err.println("Detalle (Cookies): Timeout o elemento no encontrado (mensaje de excepción nulo).");
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.err.println("Espera interrumpida durante el manejo de cookies.");
            } catch (Exception e) {
                 System.err.println("Error inesperado al intentar cerrar Cookies: " + e.getMessage());
                 e.printStackTrace();
            }
            finally {
                System.out.println("Volviendo al contenido principal de la página (después de intentar cookies)...");
                driver.switchTo().defaultContent();
            }

            // 4. Esperar a que la tabla de estadísticas de jugadores sea visible
            System.out.println("Esperando a que la tabla de estadísticas cargue...");
            By tableBodySelector = By.id("player-table-statistics-body");
            WebElement tableBody = generalWait.until(ExpectedConditions.visibilityOfElementLocated(tableBodySelector));
            System.out.println("Tabla encontrada. Extrayendo datos...");

            // 5. Extraer los datos de la tabla (sin cambios aquí)
            List<WebElement> rows = tableBody.findElements(By.tagName("tr"));
            System.out.println("Número de filas encontradas: " + rows.size());

            if (rows.isEmpty()) {
                 System.out.println("Advertencia: No se encontraron filas en la tabla.");
            }

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() >= 15) {
                    try {
                        WebElement playerInfoCell = cols.get(0);
                        String name = "Nombre no encontrado";
                        try {
                             WebElement nameSpan = playerInfoCell.findElement(By.cssSelector("a.player-link span.iconize.iconize-icon-left"));
                             name = nameSpan.getText().trim();
                             if (name.isEmpty()) {
                                 name = playerInfoCell.findElement(By.cssSelector("a.player-link")).getText().trim();
                             }
                        } catch (NoSuchElementException e) {
                             String rawText = playerInfoCell.getText();
                             String[] lines = rawText.split("\\n");
                             name = lines.length > 1 ? lines[1].trim() : lines[0].trim();
                        }

                        String matches = cols.get(4).getText().trim();
                        int goals = parseIntSafe(cols.get(6).getText());
                        int assists = parseIntSafe(cols.get(7).getText());
                        double rating = parseDoubleSafe(cols.get(14).getText());

                        System.out.println("---------------------------------------------------");
                        System.out.println("Nombre: " + name);
                        System.out.println("Partidos: " + matches);
                        System.out.println("Goles: " + goals);
                        System.out.println("Asistencias: " + assists);
                        System.out.println("Rating: " + rating);

                    } catch (Exception e) {
                        System.err.println("Error procesando una fila: " + e.getMessage());
                    }
                } else if (!cols.isEmpty()) {
                     System.out.println("Fila omitida por tener solo " + cols.size() + " columnas.");
                }
            }
            System.out.println("---------------------------------------------------");
            System.out.println("Scraping completado.");

        } catch (TimeoutException e) {
            System.err.println("Error: Timeout esperando un elemento (posiblemente tabla o pop-ups): " + e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
            System.err.println("Ocurrió un error inesperado durante el scraping:");
            e.printStackTrace();
        } finally {
            if (driver != null) {
                System.out.println("Cerrando el navegador...");
                driver.quit();
            }
        }
    }

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
            String cleanedText = text.trim().replace(",", ".").replace("-", "0.0");
            return Double.parseDouble(cleanedText);
        } catch (NumberFormatException e) { return 0.0; }
    }
}

