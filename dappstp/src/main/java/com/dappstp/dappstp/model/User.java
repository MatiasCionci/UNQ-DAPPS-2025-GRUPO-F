package com.dappstp.dappstp.model;

import java.util.UUID;
import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class User implements UserDetails{
    
    @Id 
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

    @Override
    public String getUsername() {
        return email;  // Usamos el email como el nombre de usuario
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();  // Aquí puedes añadir roles si los tienes, por ahora devolvemos una lista vacía
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;  // Por simplicidad, asumimos que la cuenta no ha expirado
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;  // Por simplicidad, asumimos que la cuenta no está bloqueada
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // Por simplicidad, asumimos que las credenciales no han expirado
    }

    @Override
    public boolean isEnabled() {
        return true;  // Por simplicidad, asumimos que la cuenta está habilitada
    }
}

