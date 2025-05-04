package com.dappstp.dappstp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Table(name = "app_user")
public class User {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true)
    private String name;

    private String password;

    public User() {
    
    }

    public Long getId() {
        return id;
    }

    public User(String name, String password) {
        this.name = name;
        this.password = password;
    }


    public String getName() { 
        return name; 
    }
    
    public String getPassword() { 
        return password; 
    }

    public void setName(String name) { 
        this.name = name; 
    }
    
    public void setPassword(String password) { 
        this.password = password; 
    }
}

