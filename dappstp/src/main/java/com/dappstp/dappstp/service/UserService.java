package com.dappstp.dappstp.service;

import com.dappstp.dappstp.model.User; // Import the User class

public interface UserService {
    User createUser(String email, String name, String password); // Method to create a new user with email and password
    User getUserByApiKey(String apiKey); // Method to retrieve a user by their API key
    boolean validateApiKey(String apiKey); // Method to validate the provided API key
}
