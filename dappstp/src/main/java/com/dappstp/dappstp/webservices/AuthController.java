package com.dappstp.dappstp.webservices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import com.dappstp.dappstp.model.AuthResponse;
import com.dappstp.dappstp.model.RegisterRequest;
import com.dappstp.dappstp.model.AuthRequest;
import com.dappstp.dappstp.model.User;
import com.dappstp.dappstp.repository.UserRepository;
import com.dappstp.dappstp.security.JwtToken;
import com.dappstp.dappstp.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/auth")
public class AuthController {
	
    @Autowired 
    private AuthenticationManager authManager;
    
    @Autowired 
    private JwtToken token;

    @Autowired 
    private UserService userService;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired private UserRepository repo;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            userService.saveNew(request);
            return ResponseEntity.ok("Usuario registrado");
        } catch (Exception e) {
            e.printStackTrace(); // Ver en logs la excepción real
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al registrar: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        User user = repo.findByName(request.getUsername())
                           .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        // Verificar la contraseña manualmente
        if (!encoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
    
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String jwtToken = token.generateToken(userDetails);
        return ResponseEntity.ok(new AuthResponse(jwtToken));
    }
}
