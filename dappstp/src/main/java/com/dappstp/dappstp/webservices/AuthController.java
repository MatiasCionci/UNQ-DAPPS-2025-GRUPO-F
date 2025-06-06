package com.dappstp.dappstp.webservices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dappstp.dappstp.model.AuthResponse;
import com.dappstp.dappstp.model.RegisterRequest;
import com.dappstp.dappstp.model.AuthRequest;
import com.dappstp.dappstp.security.JwtToken;
import com.dappstp.dappstp.service.UserService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JwtToken token;
    
    @Autowired 
    private UserService userService;


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            userService.registerUser(request.getUsername(), request.getPassword());
            return ResponseEntity.ok("Usuario registrado exitosamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al registrar usuario: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        try {
            Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String jwtToken = token.generateToken(userDetails);
            return ResponseEntity.ok(new AuthResponse(jwtToken));
        } catch (BadCredentialsException e) {
            // AuthenticationManager lanza BadCredentialsException si las credenciales son incorrectas
            // o si el usuario no se encuentra (dependiendo de la implementación de UserDetailsService).
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse("Error: Credenciales inválidas o usuario no encontrado."));
        }
    }
}
