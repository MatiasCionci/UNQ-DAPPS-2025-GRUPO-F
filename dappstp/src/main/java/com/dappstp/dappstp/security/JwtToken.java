package com.dappstp.dappstp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Component
public class JwtToken {
    
    @Value("${app.security.jwt.secret-key}")  private String secretKey;
    @Value("${app.security.expiration-time}") private long expiration; // en ms

    public String generateToken(UserDetails userDetails) {
      return Jwts.builder()
              .setSubject(userDetails.getUsername())  
              .setIssuedAt(new Date(System.currentTimeMillis()))
              .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 horas
              .signWith(getSigningKey(), SignatureAlgorithm.HS512)
              .compact();
  }

  public String extractUsername(String token) {
    return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject(); // 🔴 Esto es lo que devuelve el username
}
    public boolean validateToken(String token, UserDetails userDetails) {
      String user = extractUsername(token);
      return (user.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
      Date exp = Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                   .parseClaimsJws(token).getBody().getExpiration();
      return exp.before(new Date());
    }

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA512");
    }
}   
    

  
