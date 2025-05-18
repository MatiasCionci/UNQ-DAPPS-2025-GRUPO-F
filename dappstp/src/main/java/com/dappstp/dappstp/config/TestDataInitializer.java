package com.dappstp.dappstp.config;



import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.model.User;
import com.dappstp.dappstp.model.queryhistory.PredictionLog;
import com.dappstp.dappstp.model.scraping.StatDetailEntity;
import com.dappstp.dappstp.model.scraping.TeamStatsSummaryEntity;
import com.dappstp.dappstp.repository.PlayersRepository; // Asegúrate de tener este repositorio
import com.dappstp.dappstp.repository.PredictionLogRepository;
import com.dappstp.dappstp.repository.TeamStatsSummaryRepository;
import com.dappstp.dappstp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("dev") // Solo se ejecutará si el perfil "dev" está activo
public class TestDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TestDataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlayersRepository playersRepository; // Necesitarás crear este repositorio

    @Autowired
    private PredictionLogRepository predictionLogRepository;

    @Autowired
    private TeamStatsSummaryRepository teamStatsSummaryRepository;

    @Override
    @Transactional // Es buena idea envolver la creación de datos en una transacción
    public void run(String... args) throws Exception {
        logger.info("Iniciando la creación de datos de prueba para el perfil 'dev'...");

        // 1. Crear un usuario de prueba si no existe
        if (userRepository.findByName("testuser").isEmpty()) {
            User testUser = new User();
            testUser.setName("testuser");
            testUser.setPassword(passwordEncoder.encode("password"));
           // testUser.setRole("USER"); // O el rol que uses
            userRepository.save(testUser);
            logger.info("Usuario de prueba 'testuser' creado.");
        }

        // 2. Crear algunos jugadores de prueba si no existen
        if (playersRepository.count() == 0) {
            Players player1 = new Players("Lionel Messi", "35", 30, 20, 9.5f);
            Players player2 = new Players("Cristiano Ronaldo", "30", 40, 10, 9.3f);
            playersRepository.saveAll(List.of(player1, player2));
            logger.info("Jugadores de prueba creados.");
        }

        // 3. Crear un log de predicción de prueba
        if (predictionLogRepository.count() == 0) {
            PredictionLog log = new PredictionLog(
                "INPUT_DATA_EXAMPLE: Inter vs PSG, datos...",
                "{\"predictedWinner\":\"Inter\", \"score\":\"2-1\"}",
                "COMPREHENSIVE"
            );
            // El constructor de PredictionLog ya establece createdAt = LocalDateTime.now()
            // Si quieres una fecha específica para probar el historial:
            // log.setCreatedAt(LocalDateTime.of(2025, 5, 16, 10, 30, 0));
            predictionLogRepository.save(log);
            logger.info("Log de predicción de prueba creado.");
        }

        // 4. Crear un TeamStatsSummaryEntity de prueba
        if (teamStatsSummaryRepository.count() == 0) {
            TeamStatsSummaryEntity summary = new TeamStatsSummaryEntity();
            summary.setHomeTeamName("Paris Saint Germain");
            summary.setAwayTeamName("Inter");
            summary.setHomeTeamEmblemUrl("https://example.com/psg.png");
         


            StatDetailEntity stat1 = new StatDetailEntity("Disparos", "10", "5");
            stat1.setSummary(summary); // Establecer la relación inversa

            StatDetailEntity stat2 = new StatDetailEntity("Posesión", "58%", "42%");
            stat2.setSummary(summary); // Establecer la relación inversa
            
            summary.getStats().add(stat1); // Usa getStats() para acceder a la lista
            summary.getStats().add(stat2);

            teamStatsSummaryRepository.save(summary);
            logger.info("Resumen de estadísticas de equipo de prueba creado.");
        }

        logger.info("Datos de prueba creados/verificados exitosamente.");
    }
}
