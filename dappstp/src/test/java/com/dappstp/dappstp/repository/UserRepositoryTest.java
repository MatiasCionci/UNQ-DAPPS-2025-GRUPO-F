package com.dappstp.dappstp.repository;

import com.dappstp.dappstp.model.User; // Asegúrate que esta es la entidad correcta
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("e2e")
@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User createUser(String name, String password) {
        User user = new User(); // Asumiendo un constructor por defecto
        user.setName(name);
        user.setPassword(password); // Asumiendo que User tiene estos campos
        // user.setUserId() no es necesario si es auto-generado
        return user;
    }

    @Test
    public void whenSaveUser_thenUserIsPersisted() {
        User newUser = createUser("testuser1", "password123");

        User savedUser = userRepository.save(newUser);

        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull(); // Asumiendo que User tiene getUserId()
        assertThat(savedUser.getName()).isEqualTo("testuser1");
    }

    @Test
    public void whenFindByName_thenReturnUser() {
        User user = createUser("findMeUser", "securePass");
        entityManager.persistAndFlush(user);

        Optional<User> foundUserOpt = userRepository.findByName("findMeUser");

        assertThat(foundUserOpt).isPresent();
        assertThat(foundUserOpt.get().getName()).isEqualTo("findMeUser");
    }

    @Test
    public void whenFindByName_withNonExistingName_thenReturnEmpty() {
        Optional<User> foundUserOpt = userRepository.findByName("nonExistingUser");
        assertThat(foundUserOpt).isNotPresent();
    }

    @Test
    public void whenFindById_thenReturnUser() {
        User user = createUser("userById", "passById");
        User persistedUser = entityManager.persistFlushFind(user);

        Optional<User> foundUserOpt = userRepository.findById(persistedUser.getId());

        assertThat(foundUserOpt).isPresent();
        assertThat(foundUserOpt.get().getName()).isEqualTo("userById");
    }

    @Test
    public void whenDeleteUser_thenUserIsRemoved() {
        User user = createUser("userToDelete", "passToDelete");
        User persistedUser = entityManager.persistFlushFind(user);
        Long userId = persistedUser.getId();

        userRepository.deleteById(userId);
        entityManager.flush();
        entityManager.clear();

        Optional<User> deletedUserOpt = userRepository.findById(userId);
        assertThat(deletedUserOpt).isNotPresent();
    }
}