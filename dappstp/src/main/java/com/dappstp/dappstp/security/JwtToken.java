// Asegúrate de que esta es la clase JwtToken.java en el paquete com.dappstp.dappstp.security
package com.dappstp.dappstp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtToken {

    private static final Logger logger = LoggerFactory.getLogger(JwtToken.class); // Añadir logger

    @Value("${app.security.jwt.secret-key}")  private String secretKey;
    @Value("${app.security.expiration-time}") private long expiration; // en ms

    public String generateToken(UserDetails userDetails) {
      return Jwts.builder()
              .setSubject(userDetails.getUsername())  
              .setIssuedAt(new Date(System.currentTimeMillis()))
              // .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 horas // Comentado para usar la variable
              .setExpiration(new Date(System.currentTimeMillis() + expiration)) // Usar la expiración configurada
              .signWith(getSigningKey(), SignatureAlgorithm.HS512)
              .compact();
  }

  public Boolean validateToken(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
  }

  private Boolean isTokenExpired(String token) {
    final Date exp = extractExpiration(token);
    return exp.before(new Date());
  }

  @SuppressWarnings("removal") // Para suprimir advertencias de APIs obsoletas si las hubiera.
  private Key getSigningKey() {
      // TEMPORARY LOGGING FOR DEBUGGING - REMOVE AFTERWARDS
      logger.info("SECRET_KEY_DEBUG: Attempting to decode secretKey in getSigningKey(). Raw value from @Value: '{}'", secretKey);
      if (secretKey == null || secretKey.trim().isEmpty()) {
          logger.error("SECRET_KEY_DEBUG: JWT secretKey is null or empty. Please check environment configuration. Value was: '{}'", secretKey);
          throw new IllegalArgumentException("JWT secretKey cannot be null or empty.");
      }
      try {
          byte[] keyBytes = Base64.getDecoder().decode(secretKey);
          return new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA512");
      } catch (IllegalArgumentException e) {
          // Loguea la clave que causó el problema y el mensaje de error
          logger.error("SECRET_KEY_DEBUG: Failed to decode Base64 secret key. Key value being decoded: '{}'. Error: {}", secretKey, e.getMessage(), e);
          throw e; // rethrow the exception
      }
  }
}
