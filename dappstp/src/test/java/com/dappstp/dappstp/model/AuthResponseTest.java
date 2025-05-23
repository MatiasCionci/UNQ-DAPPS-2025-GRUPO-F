package com.dappstp.dappstp.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthResponseTest {

    @Test
    void testConstructorAndGetter() {
        String token = "sample.jwt.token";
        AuthResponse authResponse = new AuthResponse(token);

        assertEquals(token, authResponse.getToken());
    }

    // The class only has a getter and a constructor, no setters.
    // No explicit equals/hashCode/toString.
    @Test
    void testDefaultObjectMethods() {
        AuthResponse response1 = new AuthResponse("token1");
        AuthResponse response2 = new AuthResponse("token1");
        // Default equals compares references
        assertNotEquals(response1, response2);
        assertNotNull(response1.toString()); // Just check it doesn't throw
    }
}