package com.dappstp.dappstp.service.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dappstp.dappstp.model.User;
import com.dappstp.dappstp.repository.UserRepository;
import com.dappstp.dappstp.service.UserService;

@Service
public class UserServiceImpl implements UserService{
    
    @Autowired
    private UserRepository userRepository;  // Inyección del repositorio

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(String email, String name, String password) {
        String apiKey = UUID.randomUUID().toString();  // Genera una nueva apiKey
        User user = new User(email, name, password, apiKey);
        return userRepository.save(user);  // Guarda el usuario en la base de datos
    }

    @Override
    public User getUserByApiKey(String apiKey) {
        return userRepository.findByApiKey(apiKey);  // Busca un usuario por apiKey
    }

    @Override
    public boolean validateApiKey(String apiKey) {
        return userRepository.findByApiKey(apiKey) != null;  // Verifica si existe un usuario con esa apiKey
    }
}
