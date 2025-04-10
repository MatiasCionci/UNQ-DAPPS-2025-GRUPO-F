import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import java.util.List;
public class WhoScoredSeleniumScraperTeam {
    public static void main(String[] args) {
        // Asegurate de tener el driver de Chrome configurado
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\cionc\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        WebDriver driver = new ChromeDriver();
        driver.get("https://es.whoscored.com/statistics");
        

        // Esperar a que la tabla se cargue (podés mejorar esto con WebDriverWait)
        try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }

        WebElement table = driver.findElement(By.id("top-team-stats-summary-grid"));
        List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));

        for (WebElement row : rows) {
            List<WebElement> cols = row.findElements(By.tagName("td"));
            if (cols.size() < 9) continue;

            String equipo = cols.get(0).getText();
            String campeonato = cols.get(1).getText();
            String goles = cols.get(2).getText();
            String tirosPorPartido = cols.get(3).getText();
            String disciplina = cols.get(4).getText();
            String posesion = cols.get(5).getText();
            String aciertoPase = cols.get(6).getText();
            String aereos = cols.get(7).getText();
            String rating = cols.get(8).getText();

            System.out.println("Equipo: " + equipo);
            System.out.println("Campeonato: " + campeonato);
            System.out.println("Goles: " + goles);
            System.out.println("Tiros por partido: " + tirosPorPartido);
            System.out.println("Disciplina: " + disciplina);
            System.out.println("Posesión: " + posesion);
            System.out.println("Acierto de pase: " + aciertoPase);
            System.out.println("Aéreos: " + aereos);
            System.out.println("Rating: " + rating);
            System.out.println("---------------------------------------------------");
        }
        driver.quit();
    }
}

