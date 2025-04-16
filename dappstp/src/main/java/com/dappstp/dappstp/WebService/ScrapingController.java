package com.dappstp.dappstp.WebService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dappstp.dappstp.service.Scraping.PlayerProfileScrapingService;

@RestController

public class ScrapingController {
@Autowired
    private PlayerProfileScrapingService scrapingService;

    @GetMapping("/")
    public String index() {
        return "Hola, bienvenido a DappSTP!";
    }
    @GetMapping("/scrape")
    public String runScraping() {
        scrapingService.scrapeAndSavePlayer();
        return "Listo";
    }
    @GetMapping("/hello")
public String hello() {
    return "Hola desde el backend!";
}
}

