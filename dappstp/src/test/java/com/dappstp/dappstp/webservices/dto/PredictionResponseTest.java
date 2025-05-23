package com.dappstp.dappstp.webservices.dto;

import com.dappstp.dappstp.model.Prediction; // Asegúrate de importar tu clase Prediction
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PredictionResponseTest {

    @Test
    void constructorAndGetters_shouldWorkCorrectly() {
        // Puedes mockear la clase Prediction si es compleja o tiene dependencias
        Prediction mockPrediction = mock(Prediction.class);
        String status = "success";
        long timestamp = System.currentTimeMillis();

        PredictionResponse response = new PredictionResponse(mockPrediction, status, timestamp);

        assertEquals(mockPrediction, response.getPrediction(), "Prediction object should match constructor argument");
        assertEquals(status, response.getStatus(), "Status should match constructor argument");
        assertEquals(timestamp, response.getTimestamp(), "Timestamp should match constructor argument");
    }

    @Test
    void constructor_withNullPrediction_shouldStillWork() {
        // Prueba para asegurar que no hay NullPointerException si Prediction es null
        String status = "error";
        long timestamp = System.currentTimeMillis();
        PredictionResponse response = new PredictionResponse(null, status, timestamp);

        assertNull(response.getPrediction(), "Prediction should be null if passed as null");
        assertEquals(status, response.getStatus());
        assertEquals(timestamp, response.getTimestamp());
    }
}