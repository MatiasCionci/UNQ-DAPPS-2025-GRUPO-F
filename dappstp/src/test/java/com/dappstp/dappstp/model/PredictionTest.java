package com.dappstp.dappstp.model;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PredictionTest {

    @Test
    void testNoArgsConstructorAndSettersAndGetters() {
        Prediction prediction = new Prediction();

        String winner = "Team A";
        double confidence = 0.75;
        List<String> reasons = Arrays.asList("Strong offense", "Home advantage");
        String scorePrediction = "2-1";

        prediction.setWinner(winner);
        prediction.setConfidence(confidence);
        prediction.setReasons(reasons);
        prediction.setScorePrediction(scorePrediction);

        assertEquals(winner, prediction.getWinner());
        assertEquals(confidence, prediction.getConfidence());
        assertEquals(reasons, prediction.getReasons());
        assertEquals(scorePrediction, prediction.getScorePrediction());
    }

    @Test
    void testEqualsAndHashCode() {
        // Lombok @Data should handle this.
        Prediction prediction1 = new Prediction();
        prediction1.setWinner("Team X");
        prediction1.setConfidence(0.8);
        prediction1.setReasons(Arrays.asList("Reason 1", "Reason 2"));
        prediction1.setScorePrediction("3-0");

        Prediction prediction2 = new Prediction();
        prediction2.setWinner("Team X");
        prediction2.setConfidence(0.8);
        prediction2.setReasons(Arrays.asList("Reason 1", "Reason 2"));
        prediction2.setScorePrediction("3-0");

        Prediction prediction3 = new Prediction();
        prediction3.setWinner("Team Y");
        prediction3.setConfidence(0.6);
        prediction3.setReasons(Arrays.asList("Reason A"));
        prediction3.setScorePrediction("1-1");

        assertEquals(prediction1, prediction2);
        assertEquals(prediction1.hashCode(), prediction2.hashCode());
        assertNotEquals(prediction1, prediction3);
    }

    @Test
    void testToString() {
        Prediction prediction = new Prediction();
        prediction.setWinner("Team Z");
        assertNotNull(prediction.toString()); // Basic check that toString() from Lombok @Data works
    }
}