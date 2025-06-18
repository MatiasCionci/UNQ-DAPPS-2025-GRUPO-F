package com.dappstp.dappstp.security;

import java.security.SecureRandom;
import java.util.Base64;

public class GenerateSecretKey {
    public static void main(String[] args) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[64]; // Para HS512, 64 bytes (512 bits) es una buena longitud
        secureRandom.nextBytes(key);
        String base64Key = Base64.getEncoder().encodeToString(key);
      //  System.out.println("Generated Base64 Key: " + base64Key);
    }
}
