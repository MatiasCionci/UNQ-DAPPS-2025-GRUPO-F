package com.dappstp.dappstp.config;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class ApiPathsTest {

    @Test
    void apiBase_shouldHaveCorrectValue() {
        assertEquals("/api", ApiPaths.API_BASE, "API_BASE constant should have the correct value");
    }

    @Test
    void predictionsPp_shouldHaveCorrectValue() {
        assertEquals("/api/predictionspp", ApiPaths.PREDICTIONS_PP, "PREDICTIONS_PP constant should have the correct value");
    }

    @Test
    void constructor_shouldBePrivateAndThrowException() throws NoSuchMethodException {
        Constructor<ApiPaths> constructor = ApiPaths.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");

        constructor.setAccessible(true); // Necesario para invocar un constructor privado

        // Since the constructor now throws UnsupportedOperationException,
        // invoking it via reflection will result in an InvocationTargetException.
        assertThrows(InvocationTargetException.class, constructor::newInstance, "Instantiating utility class should throw an InvocationTargetException wrapping an UnsupportedOperationException.");
    }
}