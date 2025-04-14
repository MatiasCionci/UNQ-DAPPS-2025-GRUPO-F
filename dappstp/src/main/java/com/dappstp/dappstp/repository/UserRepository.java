package com.dappstp.dappstp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dappstp.dappstp.model.User; 

public interface UserRepository extends JpaRepository<User, Long> {

    User findByApiKey(String apiKey);  // Busca un usuario por su API key
}
