package com.dappstp.dappstp.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class User {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private UUID id;

    private String name;
    private String email;
    private String password;
    private String apiKey;

    public User() {
        this.id = UUID.randomUUID();
    }

    public User(String email, String name, String password, String apiKey) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.apiKey = apiKey;
    }

    public UUID getId() { 
        return id; 
    }

    public String getName() { 
        return name; 
    }
    
    public String getEmail() { 
        return email; 
    }
    
    public String getPassword() { 
        return password; 
    }
    
    public String getApiKey() { 
        return apiKey; 
    }

    public void setEmail(String email) { 
        this.email = email; 
    }

    public void setName(String name) { 
        this.name = name; 
    }
    
    public void setPassword(String password) { 
        this.password = password; 
    }
    
    public void setApiKey(String apiKey) { 
        this.apiKey = apiKey; 
    }
}

