package com.dappstp.dappstp.security;

import java.util.Date;
import java.security.Key;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtToken {
    
    private final String secretKey;
    private final Long expirationDate;

    public JwtToken(@Value("${app.security.jwt.secret-key}") String secretKey,
                    @Value("${app.security.expiration-time}") Long expirationDate){
        this.secretKey = secretKey;
        this.expirationDate = expirationDate;
    }

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS512.getJcaName());
    }

    public String generateToken(Authentication authentication){
        Date now = new Date();
        Date expirationToken = new Date(now.getTime() + expirationDate);

        return Jwts.builder()
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .setSubject(authentication.getName()) 
                .setIssuedAt(new Date())
                .setExpiration(expirationToken)
                .compact();
    }

    public String getEmailFromJwt (String token){
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

}   
