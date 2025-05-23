package com.dappstp.dappstp.repository;

import com.dappstp.dappstp.model.Players;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager; // Opcional, para un control más fino

import java.util.List;
import java.util.Optional;
import org.springframework.test.context.ActiveProfiles; // Para activar el perfil de prueba
import static org.assertj.core.api.Assertions.assertThat; // Usando AssertJ para aserciones más legibles
@ActiveProfiles("e2e")
@DataJpaTest // Configura el entorno de prueba para JPA
public class PlayersRepositoryTest {

    @Autowired
    private PlayersRepository playersRepository;

    // TestEntityManager es una alternativa a usar directamente el repositorio
    // para preparar datos de prueba, especialmente útil para relaciones o IDs generados.
    @Autowired(required = false) // Hazlo opcional si no siempre lo usas
    private TestEntityManager entityManager;

    @Test
    public void whenSavePlayer_thenPlayerIsPersisted() {
        // Given: un nuevo jugador
        Players newPlayer = new Players("Test Player", "10(2)", 5, 2, 7.5);

        // When: se guarda el jugador
        Players savedPlayer = playersRepository.save(newPlayer);

        // Then: el jugador guardado no es nulo y tiene un ID asignado
        assertThat(savedPlayer).isNotNull();
        assertThat(savedPlayer.getId()).isNotNull();
        assertThat(savedPlayer.getName()).isEqualTo("Test Player");
    }

    @Test
    public void whenFindById_thenReturnPlayer() {
        // Given: un jugador persistido
        Players player = new Players("Findable Player", "5(0)", 2, 1, 6.0);
        Players persistedPlayer = entityManager != null ? entityManager.persistFlushFind(player) : playersRepository.save(player);


        // When: se busca el jugador por ID
        Optional<Players> foundPlayerOpt = playersRepository.findById(persistedPlayer.getId());

        // Then: el jugador es encontrado
        assertThat(foundPlayerOpt).isPresent();
        assertThat(foundPlayerOpt.get().getName()).isEqualTo("Findable Player");
    }

    @Test
    public void whenFindAll_thenReturnAllPlayers() {
        // Given: algunos jugadores persistidos
        Players player1 = new Players("Player One", "1(0)", 0, 0, 5.0);
        Players player2 = new Players("Player Two", "2(0)", 1, 0, 5.5);
        if (entityManager != null) {
            entityManager.persist(player1);
            entityManager.persist(player2);
            entityManager.flush();
        } else {
            playersRepository.save(player1);
            playersRepository.save(player2);
        }


        // When: se buscan todos los jugadores
        List<Players> allPlayers = playersRepository.findAll();

        // Then: la lista contiene los jugadores (el tamaño puede variar si otros tests afectan la BD
        // y no hay un @DirtiesContext o limpieza manual, pero @DataJpaTest hace rollback)
        assertThat(allPlayers).hasSizeGreaterThanOrEqualTo(2); // Ajusta según tu setup
        assertThat(allPlayers).extracting(Players::getName).contains("Player One", "Player Two");
    }

    @Test
    public void whenUpdatePlayer_thenChangesArePersisted() {
        // Given: un jugador persistido
        Players player = new Players("Updatable Player", "3(1)", 1, 1, 6.5);
        Players persistedPlayer = entityManager != null ? entityManager.persistFlushFind(player) : playersRepository.save(player);

        // When: se actualiza el nombre del jugador y se guarda
        Players playerToUpdate = playersRepository.findById(persistedPlayer.getId()).orElseThrow();
        playerToUpdate.setName("Updated Player Name");
        playersRepository.save(playerToUpdate);

        // Then: el jugador recuperado tiene el nombre actualizado
        Optional<Players> updatedPlayerOpt = playersRepository.findById(persistedPlayer.getId());
        assertThat(updatedPlayerOpt).isPresent();
        assertThat(updatedPlayerOpt.get().getName()).isEqualTo("Updated Player Name");
    }

    @Test
    public void whenDeletePlayer_thenPlayerIsRemoved() {
        // Given: un jugador persistido
        Players player = new Players("Deletable Player", "1(1)", 0, 1, 5.8);
        Players persistedPlayer = entityManager != null ? entityManager.persistFlushFind(player) : playersRepository.save(player);
        Long playerId = persistedPlayer.getId();

        // When: se elimina el jugador
        playersRepository.deleteById(playerId);
         if (entityManager != null) {
            entityManager.flush(); // Asegura que la eliminación se propague
            entityManager.clear(); // Desvincula las entidades para que findById realmente vaya a la BD
        }


        // Then: el jugador ya no se encuentra
        Optional<Players> deletedPlayerOpt = playersRepository.findById(playerId);
        assertThat(deletedPlayerOpt).isNotPresent();
    }
}
