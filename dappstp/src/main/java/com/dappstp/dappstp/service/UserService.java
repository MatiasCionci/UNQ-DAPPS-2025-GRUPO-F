package com.dappstp.dappstp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.dappstp.dappstp.model.User; 
import com.dappstp.dappstp.model.RegisterRequest; 
import org.springframework.http.HttpStatus;

import com.dappstp.dappstp.repository.UserRepository;

@Service
public class UserService implements UserDetailsService {

    @Autowired private UserRepository repo;
    @Autowired private PasswordEncoder encoder;

    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
      User user = repo.findByName(name)
                   .orElseThrow(() -> new UsernameNotFoundException("No existe usuario"));
      return new org.springframework.security.core.userdetails.User(
               user.getName(), user.getPassword(), new ArrayList<>());
    }

    public User saveNew(RegisterRequest request) {
      System.out.println("Intentando registrar usuario: " + request.getUsername());
  
      if (repo.findByName(request.getUsername()).isPresent()) {
          System.out.println("Usuario ya existe");
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario ya existe");
      }
  
      User user = new User();
      user.setName(request.getUsername());
      user.setPassword(encoder.encode(request.getPassword()));
      
      User saved = repo.save(user);
      System.out.println("Usuario guardado con ID: " + saved.getId());
      return saved;
  }
}
