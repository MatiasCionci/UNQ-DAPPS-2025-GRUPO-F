package com.dappstp.dappstp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dappstp.dappstp.model.User; 

@Repository
public interface UserRepository extends JpaRepository<User, java.util.UUID> {

    User findByApiKey(String apiKey);  // Busca un usuario por su API key

    User findByEmail(String email);
}
