package com.dappstp.dappstp.service.scraping.aspect;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import com.dappstp.dappstp.service.scraping.aspect.annotation.EnableScrapingSession;
import com.dappstp.dappstp.service.scraping.aspect.context.ScrapingContext;
import com.dappstp.dappstp.service.scraping.aspect.context.ScrapingContextHolder;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Aspect
@Component
@Slf4j
public class WebDriverManagementAspect {

    private final Random random = new Random();
    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.5845.183 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Safari/605.1.15",
            "Mozilla/5.0 (X11; Linux x86_64; rv:102.0) Gecko/20100101 Firefox/102.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/18.19041"
    );

    @Around("@annotation(enableScrapingSession)")
    public Object manageWebDriver(ProceedingJoinPoint joinPoint, EnableScrapingSession enableScrapingSession) throws Throwable {
        log.info("🚀 AOP: Iniciando sesión de scraping con WebDriver gestionado...");
        WebDriverManager.chromedriver().setup();
        String ua = USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
        log.debug("AOP: User‑Agent seleccionado: {}", ua);

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NONE);
        options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080",
                "--user-agent=" + ua,
                "--user-data-dir=/tmp/chrome-profile-" + UUID.randomUUID()
        );

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60)); // Default wait time
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(180));

            ScrapingContextHolder.setContext(new ScrapingContext(driver, wait));
            log.info("AOP: WebDriver y WebDriverWait configurados y listos.");

            return joinPoint.proceed();

        } catch (Exception e) {
            log.error("AOP: Error durante la ejecución del método de scraping: {}", e.getMessage(), e);
            throw e; // Re-throw la excepción para que el llamador original la maneje si es necesario
        } finally {
            if (ScrapingContextHolder.getContext() != null && ScrapingContextHolder.getContext().getDriver() != null) {
                log.info("AOP: Cerrando WebDriver...");
                ScrapingContextHolder.getContext().getDriver().quit();
            }
            ScrapingContextHolder.clearContext();
            log.info("🏁 AOP: Sesión de scraping finalizada, WebDriver cerrado y contexto limpiado.");
        }
    }
}