package com.dappstp.dappstp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        appConfig = new AppConfig();
    }

    @Test
    void objectMapper_shouldBeConfiguredCorrectly() {
        ObjectMapper objectMapper = appConfig.objectMapper();

        assertNotNull(objectMapper, "ObjectMapper should not be null");

        // Verificar que JavaTimeModule está registrado
        assertTrue(objectMapper.getRegisteredModuleIds().contains(new JavaTimeModule().getTypeId()),
                "JavaTimeModule should be registered");

        // Verificar que WRITE_DATES_AS_TIMESTAMPS está deshabilitado
        assertFalse(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS),
                "WRITE_DATES_AS_TIMESTAMPS should be disabled");
    }

    @Test
    void restTemplate_shouldBeCreated() {
        RestTemplate restTemplate = appConfig.restTemplate();
        assertNotNull(restTemplate, "RestTemplate should not be null");
    }

    @Test
    void customOpenAPI_shouldBeConfiguredCorrectly() {
        OpenAPI openAPI = appConfig.customOpenAPI();
        assertNotNull(openAPI, "OpenAPI should not be null");

        Info info = openAPI.getInfo();
        assertNotNull(info, "OpenAPI Info should not be null");
        assertEquals("DappSTP API", info.getTitle(), "Title should match");
        assertEquals("v0.0.1", info.getVersion(), "Version should match");
        assertEquals("API para predicciones deportivas y scraping de datos.", info.getDescription(), "Description should match");
    }
}