package com.dappstp.dappstp.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthRequestTest {

    @Test
    void testConstructorAndGetters() {
        String username = "testuser";
        String password = "password123";
        AuthRequest authRequest = new AuthRequest(username, password);

        assertEquals(username, authRequest.getUsername());
        assertEquals(password, authRequest.getPassword());
    }

    @Test
    void testSetters() {
        AuthRequest authRequest = new AuthRequest(null, null); // Start with null or default

        String newUsername = "anotheruser";
        String newPassword = "newPassword";

        authRequest.setUsername(newUsername);
        authRequest.setPassword(newPassword);

        assertEquals(newUsername, authRequest.getUsername());
        assertEquals(newPassword, authRequest.getPassword());
    }

    // No explicit equals/hashCode/toString in the class, so testing default Object behavior or skipping.
    // If Lombok @Data were added, these tests would be more relevant.
    @Test
    void testDefaultObjectMethods() {
        AuthRequest authRequest1 = new AuthRequest("user", "pass");
        AuthRequest authRequest2 = new AuthRequest("user", "pass");
        assertNotEquals(authRequest1, authRequest2, "Default equals should compare references");
    }
}
