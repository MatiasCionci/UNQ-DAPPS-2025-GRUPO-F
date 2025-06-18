package com.dappstp.dappstp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.dappstp.dappstp.model.User; 
// Import RegisterRequest ya no es necesario para el método registerUser si pasas strings directamente
// import com.dappstp.dappstp.model.RegisterRequest; 
import org.springframework.http.HttpStatus;

import com.dappstp.dappstp.repository.UserRepository;

@Service
public class UserService implements UserDetailsService {

    @Autowired private UserRepository repo;
    @Autowired private PasswordEncoder encoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
      User user = repo.findByName(name)
                   .orElseThrow(() -> new UsernameNotFoundException("No existe usuario"));
      return new org.springframework.security.core.userdetails.User(
               user.getName(), user.getPassword(), new ArrayList<>());
    }

    // Método modificado para aceptar username y password directamente
    public User registerUser(String username, String password) {
      logger.info("Intentando registrar usuario: " + username);
  
      if (repo.findByName(username).isPresent()) {
          logger.info("Usuario ya existe");
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario ya existe");
      }
  
      User user = new User();
      user.setName(username);
      user.setPassword(encoder.encode(password));
      
      User saved = repo.save(user);
      logger.info("Usuario guardado con ID: " + saved.getId());
      return saved;
  }
}
