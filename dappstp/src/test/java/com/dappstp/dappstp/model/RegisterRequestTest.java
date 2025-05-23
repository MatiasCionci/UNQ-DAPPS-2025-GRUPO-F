package com.dappstp.dappstp.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    @Test
    void testGettersAndSetters() {
        RegisterRequest registerRequest = new RegisterRequest();

        String username = "newuser";
        String password = "securePassword123";

        registerRequest.setUsername(username);
        registerRequest.setPassword(password);

        assertEquals(username, registerRequest.getUsername());
        assertEquals(password, registerRequest.getPassword());
    }

    @Test
    void testDefaultConstructor() {
        RegisterRequest registerRequest = new RegisterRequest();
        assertNull(registerRequest.getUsername());
        assertNull(registerRequest.getPassword());
    }

    // No explicit equals/hashCode/toString in the class.
    // If Lombok @Data were added, these tests would be more relevant.
    @Test
    void testDefaultObjectMethods() {
        RegisterRequest request1 = new RegisterRequest();
        request1.setUsername("user1");
        RegisterRequest request2 = new RegisterRequest();
        request2.setUsername("user1");
        assertNotEquals(request1, request2, "Default equals should compare references");
    }
}