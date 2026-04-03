package com.user.User.Service.repository;

import com.user.User.Service.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    // ---- save ----

    @Test
    void save_ShouldPersistUser() {
        User user = new User(null, "mn3118110@gmail.com", "password", "Manjunath", "Gowda");

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("mn3118110@gmail.com", saved.getEmail());
        assertEquals("Manjunath", saved.getFirstName());
        assertEquals("Gowda", saved.getLastName());
    }

    // ---- findById ----

    @Test
    void findById_WhenUserExists_ShouldReturnUser() {
        User user = new User(null, "ravi@gmail.com", "password", "Ravi", "Kumar");
        User persisted = userRepository.save(user);

        Optional<User> found = userRepository.findById(persisted.getId());

        assertTrue(found.isPresent());
        assertEquals("Ravi", found.get().getFirstName());
        assertEquals("Kumar", found.get().getLastName());
    }

    @Test
    void findById_WhenUserDoesNotExist_ShouldReturnEmpty() {
        Optional<User> found = userRepository.findById(999L);

        assertFalse(found.isPresent());
    }

    // ---- findAll ----

    @Test
    void findAll_ShouldReturnAllUsers() {
        userRepository.save(new User(null, "mn3118110@gmail.com", "password", "Manjunath", "Gowda"));
        userRepository.save(new User(null, "ravi@gmail.com", "password", "Ravi", "Kumar"));
        userRepository.save(new User(null, "priya@gmail.com", "password", "Priya", "Sharma"));

        List<User> users = userRepository.findAll();

        assertEquals(3, users.size());
    }

    @Test
    void findAll_WhenNoUsers_ShouldReturnEmptyList() {
        List<User> users = userRepository.findAll();

        assertTrue(users.isEmpty());
    }

    // ---- delete ----

    @Test
    void delete_ShouldRemoveUser() {
        User user = userRepository.save(new User(null, "mn3118110@gmail.com", "password", "Manjunath", "Gowda"));

        userRepository.delete(user);

        Optional<User> deleted = userRepository.findById(user.getId());
        assertFalse(deleted.isPresent());
    }

    // ---- update (save existing) ----

    @Test
    void save_ExistingUser_ShouldUpdateFields() {
        User user = userRepository.save(new User(null, "mn3118110@gmail.com", "password", "Manjunath", "Gowda"));

        user.setFirstName("Manju Updated");
        user.setLastName("G");
        user.setEmail("updated@gmail.com");
        userRepository.save(user);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("Manju Updated", updated.getFirstName());
        assertEquals("G", updated.getLastName());
        assertEquals("updated@gmail.com", updated.getEmail());
    }
}

