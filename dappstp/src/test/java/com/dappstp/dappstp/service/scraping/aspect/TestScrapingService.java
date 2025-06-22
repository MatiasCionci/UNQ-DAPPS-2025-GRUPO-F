package com.dappstp.dappstp.service.scraping.aspect;
import com.dappstp.dappstp.aspect.scraping.annotation.EnableScrapingSession;
import com.dappstp.dappstp.aspect.scraping.context.ScrapingContext;
import com.dappstp.dappstp.aspect.scraping.context.ScrapingContextHolder;
import lombok.Getter;
import lombok.Setter;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Service;

@Service
public class TestScrapingService {

    @Getter
    @Setter
    private boolean methodExecuted = false;
    @Getter
    @Setter
    private WebDriver driverInMethod = null;

    @EnableScrapingSession
    public String performScrapingAction() {
        this.methodExecuted = true;
        ScrapingContext context = ScrapingContextHolder.getContext();
        if (context != null) {
            this.driverInMethod = context.getDriver();
        }
        return "Scraping action performed";
    }

    @EnableScrapingSession
    public void performScrapingActionThatThrows() throws Exception {
        this.methodExecuted = true;
        throw new Exception("Test exception from scraping method");
    }
}