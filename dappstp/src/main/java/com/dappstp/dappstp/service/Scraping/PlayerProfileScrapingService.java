package com.dappstp.dappstp.service.Scraping;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dappstp.dappstp.model.PlayerProfileScraping;
import com.dappstp.dappstp.repository.PlayersProfileRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PlayerProfileScrapingService {
 @Autowired
    private PlayersProfileRepository playerProfileRepository;

    public void scrapeAndSavePlayer() {
        // Initialize the Chrome WebDriver
        WebDriver driver = new ChromeDriver();
        driver.get("https://es.whoscored.com/players/480249/show/lamine-yamal");

        try {
            // Get the container element by retrieving the parent element using XPath ".."
            WebElement container = driver.findElement(By.cssSelector(".player-picture-container"))
                    .findElement(By.xpath(".."));

            // Extract data using XPath and CSS selectors
            String name = container.findElement(By.xpath(".//span[text()='Nombre: ']/following-sibling::text()[1]")).getText();
            String fullName = container.findElement(By.xpath(".//span[text()='Nombre Completo: ']/parent::div")).getText()
                    .replace("Nombre Completo: ", "");
            String team = container.findElement(By.cssSelector("a.team-link")).getText();
            int shirtNumber = Integer.parseInt(
                    container.findElement(By.xpath(".//span[text()='Número de Dorsal: ']/parent::div")).getText()
                            .replace("Número de Dorsal: ", ""));
            String ageText = container.findElement(By.xpath(".//span[text()='Edad: ']/parent::div")).getText();
            int age = Integer.parseInt(ageText.replaceAll("[^\\d]", "").substring(0, 2));
            String birthDateText = container.findElement(By.xpath(".//i")).getText();
            int height = Integer.parseInt(container.findElement(By.xpath(".//span[text()='Altura: ']/parent::div")).getText()
                    .replace("Altura: ", "").replace("cm", ""));
            String nationality = container.findElement(By.xpath(".//span[text()='Nacionalidad: ']/parent::div")).getText()
                    .replace("Nacionalidad: ", "").split(" ")[0];
            String position = container.findElement(By.xpath(".//span[text()='Posiciones: ']/following-sibling::span"))
                    .getText();

            // Parse the birth date using the defined formatter
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate birthDate = LocalDate.parse(birthDateText, formatter);

            // Create a new PlayerProfileScraping object and set its properties
            PlayerProfileScraping player = new PlayerProfileScraping();
            player.setName(name);
            player.setFullName(fullName);
            player.setCurrentTeam(team);
            player.setShirtNumber(shirtNumber);
            player.setAge(age);
            player.setBirthDate(birthDate);
            player.setHeight(height);
            player.setNationality(nationality);
            player.setPosition(position);

            // Save the player profile using the repository
            playerProfileRepository.save(player);
            System.out.println("Player profile saved successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Quit the driver to free resources
            driver.quit();
        }
    }
}

