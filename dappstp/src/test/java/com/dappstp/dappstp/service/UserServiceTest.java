package com.dappstp.dappstp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dappstp.dappstp.model.User;
import com.dappstp.dappstp.service.impl.UserServiceImpl;
import com.dappstp.dappstp.repository.UserRepository;

import org.junit.jupiter.api.Test;

class UserServiceTest {
    
    @Test
    void testCreateUser_GeneratesApiKeyAndSavesUser() {

        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        String email = "usuario@test.com";
        String password = "123456";
        String name = "Nicolas";

        when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.createUser(email, name, password);

        assertNotNull(user.getApiKey());
        assertEquals(email, user.getEmail());
        assertEquals(password, user.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testValidateApiKey() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.createUser("email@test.com", "Test", "pass123");
        String apiKey = user.getApiKey();

        when(userRepository.findByApiKey(apiKey)).thenReturn(user);

        assertTrue(userService.validateApiKey(apiKey));
    }

    @Test
    void testValidateNotApiKey() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        assertFalse(userService.validateApiKey("inexistente-api-key"));
    }

    @Test
    void testGetUserByApiKey() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.createUser("otro@test.com", "Otro User", "clave321");
        String apiKey = user.getApiKey();

        when(userRepository.findByApiKey(apiKey)).thenReturn(user);

        User fetchedUser = userService.getUserByApiKey(apiKey);

        assertNotNull(fetchedUser);
        assertEquals(user.getEmail(), fetchedUser.getEmail());
        assertEquals(user.getName(), fetchedUser.getName());
    }

    @Test
    void testGetUserNotByApiKey() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        User fetchedUser = userService.getUserByApiKey("apiKeyInvalida");

        assertNull(fetchedUser);
    }
}
