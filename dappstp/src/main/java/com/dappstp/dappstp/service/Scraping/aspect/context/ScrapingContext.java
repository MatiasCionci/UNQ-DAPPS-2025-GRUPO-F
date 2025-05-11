package com.dappstp.dappstp.service.Scraping.aspect.context;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ScrapingContext {
    private final WebDriver driver;
    private final WebDriverWait wait;

    public ScrapingContext(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public WebDriver getDriver() {
        return driver;
    }

    public WebDriverWait getWait() {
        return wait;
    }
}