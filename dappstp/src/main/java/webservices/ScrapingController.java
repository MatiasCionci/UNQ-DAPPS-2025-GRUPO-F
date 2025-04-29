package webservices; // Asegúrate que el paquete sea correcto para tu estructura
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.service.Scraping.ScraperServicePlayers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api") // Ruta base para todos los endpoints de esta API
public class ScrapingController {

    private final ScraperServicePlayers scraperService;

    private static final Logger logger = LoggerFactory.getLogger(ScrapingController.class);

    // Inyección de dependencias vía constructor para ambos servicios
    @Autowired
    public ScrapingController(ScraperServicePlayers scraperService) {
 
        this.scraperService = scraperService;
    }

    @GetMapping("/")
    public String index() {
        return "API DappSTP - Endpoints de Scraping disponibles en /api/scrape/...";
    }

    @GetMapping("/players")
    public String getPlayers() {
        logger.info("➡️ Entró al endpoint /players");

        List<Players> players = scraperService.scrapeAndSavePlayers();
    
        logger.info("🧠 Scrap completado. Jugadores encontrados: {}", players.size());
    
        StringBuilder response = new StringBuilder("Jugadores:\n");
        players.forEach(p -> {
            response.append(p.getName())
                    .append(" - ")
                    .append(p.getMatches())
                    .append(" - ")
                    .append(p.getGoals())
                    .append(" - ")
                    .append(p.getAssists())
                    .append(" - ")
                    .append(p.getRating())
                    .append("\n");
        });
    
        return response.toString();
    }

   
    @GetMapping("/hello")
     public String hello() {
       return "Hola desde el backend!";
    }
}


