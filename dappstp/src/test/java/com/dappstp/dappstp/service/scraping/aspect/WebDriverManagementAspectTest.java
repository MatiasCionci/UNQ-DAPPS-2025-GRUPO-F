package com.dappstp.dappstp.service.scraping.aspect;
import com.dappstp.dappstp.service.scraping.aspect.context.ScrapingContextHolder;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {WebDriverManagementAspect.class, TestScrapingService.class})
@EnableAspectJAutoProxy // Asegura que AOP esté habilitado para el test
class WebDriverManagementAspectTest {

    @Autowired
    private TestScrapingService testScrapingService;

    // WebDriverManager puede necesitar una configuración única si se ejecuta en paralelo o en CI.
    // Para tests locales secuenciales, esto suele ser suficiente.
    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup(); // Configura el driver una vez para todos los tests
    }

    @BeforeEach
    void setUp() {
        // Asegurar que el contexto esté limpio antes de cada test
        ScrapingContextHolder.clearContext();
        testScrapingService.setMethodExecuted(false); // Resetear el estado del servicio de prueba
        testScrapingService.setDriverInMethod(null);
    }

    @AfterEach
    void tearDown() {
        // Asegurar que el contexto esté limpio después de cada test
        ScrapingContextHolder.clearContext();
    }

    @Test
    void whenMethodWithEnableScrapingSessionIsCalled_thenWebDriverIsManaged() {
        // Pre-condición: El contexto debe estar vacío
        assertNull(ScrapingContextHolder.getContext(), "Context should be null before execution");

        // Act
        String result = testScrapingService.performScrapingAction();

        // Assert
        assertEquals("Scraping action performed", result);
        assertTrue(testScrapingService.isMethodExecuted(), "Scraping method should have been executed");

        // Verificar que el contexto se limpió después de la ejecución
        assertNull(ScrapingContextHolder.getContext(), "Context should be cleared after execution");

        // Verificar que un driver fue accesible dentro del método
        assertNotNull(testScrapingService.getDriverInMethod(), "WebDriver should have been available in the method");

        // Es difícil verificar directamente que driver.quit() fue llamado sin mockear WebDriver
        // o inspeccionar logs, pero el aspecto debería haberlo intentado.
        // Si el driver no se cerrara, podría causar problemas en ejecuciones subsecuentes o dejar procesos.
    }

    @Test
    void whenMethodWithEnableScrapingSessionThrowsException_thenWebDriverIsStillClosedAndContextCleared() {
        // Pre-condición: El contexto debe estar vacío
        assertNull(ScrapingContextHolder.getContext(), "Context should be null before execution");

        // Act & Assert
        Exception thrownException = assertThrows(Exception.class, () -> {
            testScrapingService.performScrapingActionThatThrows();
        }, "Expected an exception to be thrown by the service method");

        assertEquals("Test exception from scraping method", thrownException.getMessage());
        assertTrue(testScrapingService.isMethodExecuted(), "Scraping method should have been executed even if it threw an exception");

        // Verificar que el contexto se limpió después de la ejecución, incluso con excepción
        assertNull(ScrapingContextHolder.getContext(), "Context should be cleared after execution even with exception");
    }

    // Helper para resetear el estado del servicio de prueba (si no se usa @DirtiesContext)
    // Esto se hace ahora en el @BeforeEach
    // private void resetTestServiceState() {
    //     java.lang.reflect.Field methodExecutedField = TestScrapingService.class.getDeclaredField("methodExecuted");
    //     methodExecutedField.setAccessible(true);
    //     methodExecutedField.set(testScrapingService, false);
    // }
}