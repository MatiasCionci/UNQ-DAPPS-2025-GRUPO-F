
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WhoScoredSeleniumScraperPlayers {
   
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\cionc\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");
        // Configurar WebDriverManager (opcional, para gestionar el driver automáticamente)
       
        WebDriver driver = new ChromeDriver();
    
        try {
            // Abrir la página deseada
            driver.get("https://es.whoscored.com/statistics");
    
            // Esperar a que la tabla esté visible
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.id("top-player-stats-summary-grid")));
    
            // Una vez localizada la tabla, obtenemos las filas del tbody
            WebElement tbody = table.findElement(By.tagName("tbody"));
            List<WebElement> rows = tbody.findElements(By.tagName("tr"));
    
            // Recorrer cada fila y extraer datos
            System.out.println("Datos extraídos de la tabla:");
            for (WebElement row : rows) {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                
                // Por ejemplo, imprimimos el contenido de cada celda
                for (WebElement cell : cells) {
                    System.out.print(cell.getText().trim() + "\t");
                }
                System.out.println();
            }
    
        } catch(Exception e) {
            System.err.println("Error durante el scraping: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cerramos el navegador
            driver.quit();
        }
    }
}
