package com.dappstp.dappstp.security;
import ch.qos.logback.classic.LoggerContext;
import java.security.SecureRandom;
import java.util.Base64;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
//probando para ver si guarga en logs
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.util.StatusPrinter;

import org.slf4j.LoggerFactory;
public class GenerateSecretKey {
  
      private static final Logger logger = LoggerFactory.getLogger("AuditLogger");

    public static void main(String[] args) {
     //  LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
       // StatusPrinter.print(lc);
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[64]; // Para HS512, 64 bytes (512 bits) es una buena longitud
        secureRandom.nextBytes(key);
        String base64Key = Base64.getEncoder().encodeToString(key);
      //  System.out.println("Generated Base64 Key: " + base64Key);
      logger.info("Generated Base64 Key: {}", base64Key);
        // Aquí puedes guardar la clave en un lugar seguro, como una base de datos o un archivo de configuración
        // Por ejemplo, podrías usar un servicio de gestión de secretos como AWS Secrets Manager o Azure Key Vault
        // para almacenar esta clave de forma segura.
       //   System.out.println("Log debería estar en: " + System.getProperty("user.dir") + "/logs/auditoria.log");
      //  System.out.println("Ruta de log: " + new File("logs/auditoria.log").getAbsolutePath());

    }
}
