package com.user.User.Service.integration;

import com.user.User.Service.repository.UserRepository;
import com.user.User.Service.user.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/users")
                .build();
        userRepository.deleteAll();
    }

    // ---- POST /users ----

    @Test
    @Order(1)
    void createUser_ShouldReturnCreatedUserWith201() {
        User created = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "mn3118110@gmail.com", "password", "Manjunath", "Gowda"))
                .retrieve()
                .body(User.class);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("mn3118110@gmail.com", created.getEmail());
        assertEquals("Manjunath", created.getFirstName());
        assertEquals("Gowda", created.getLastName());
        assertEquals(1, userRepository.count());
    }

    // ---- GET /users ----

    @Test
    @Order(2)
    void getAllUsers_ShouldReturnAllUsers() {
        userRepository.save(new User(null, "mn3118110@gmail.com", "password", "Manjunath", "Gowda"));
        userRepository.save(new User(null, "ravi@gmail.com", "password", "Ravi", "Kumar"));

        User[] users = restClient.get()
                .retrieve()
                .body(User[].class);

        assertNotNull(users);
        assertEquals(2, users.length);
        assertEquals("Manjunath", users[0].getFirstName());
        assertEquals("Ravi", users[1].getFirstName());
    }

    @Test
    @Order(3)
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        User[] users = restClient.get()
                .retrieve()
                .body(User[].class);

        assertNotNull(users);
        assertEquals(0, users.length);
    }

    // ---- GET /users/{id} ----

    @Test
    @Order(4)
    void getUserById_WhenUserExists_ShouldReturnUser() {
        User saved = userRepository.save(new User(null, "mn3118110@gmail.com", "password", "Manjunath", "Gowda"));

        User found = restClient.get()
                .uri("/" + saved.getId())
                .retrieve()
                .body(User.class);

        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals("Manjunath", found.getFirstName());
        assertEquals("mn3118110@gmail.com", found.getEmail());
    }

    @Test
    @Order(5)
    void getUserById_WhenUserDoesNotExist_ShouldReturn404() {
        assertThrows(HttpClientErrorException.NotFound.class, () ->
                restClient.get()
                        .uri("/999")
                        .retrieve()
                        .body(User.class));
    }

    // ---- PUT /users/{id} ----

    @Test
    @Order(6)
    void updateUser_WhenUserExists_ShouldReturnUpdatedUser() {
        User saved = userRepository.save(new User(null, "mn3118110@gmail.com", "password", "Manjunath", "Gowda"));

        User updated = restClient.put()
                .uri("/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "updated@gmail.com", "newpass", "Manju Updated", "G"))
                .retrieve()
                .body(User.class);

        assertNotNull(updated);
        assertEquals("Manju Updated", updated.getFirstName());
        assertEquals("G", updated.getLastName());
        assertEquals("updated@gmail.com", updated.getEmail());

        // Verify in DB
        User fromDb = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Manju Updated", fromDb.getFirstName());
        assertEquals("updated@gmail.com", fromDb.getEmail());
    }

    @Test
    @Order(7)
    void updateUser_WhenUserDoesNotExist_ShouldReturn404() {
        assertThrows(HttpClientErrorException.NotFound.class, () ->
                restClient.put()
                        .uri("/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new User(null, "ghost@gmail.com", "pass", "Ghost", "User"))
                        .retrieve()
                        .body(User.class));
    }

    // ---- DELETE /users/{id} ----

    @Test
    @Order(8)
    void deleteUser_WhenUserExists_ShouldReturn204() {
        User saved = userRepository.save(new User(null, "mn3118110@gmail.com", "password", "Manjunath", "Gowda"));

        restClient.delete()
                .uri("/" + saved.getId())
                .retrieve()
                .toBodilessEntity();

        assertFalse(userRepository.findById(saved.getId()).isPresent());
    }

    @Test
    @Order(9)
    void deleteUser_WhenUserDoesNotExist_ShouldReturn404() {
        assertThrows(HttpClientErrorException.NotFound.class, () ->
                restClient.delete()
                        .uri("/999")
                        .retrieve()
                        .toBodilessEntity());
    }

    // ---- Full CRUD flow end-to-end ----

    @Test
    @Order(10)
    void fullCrudFlow_ShouldWorkEndToEnd() {
        // CREATE
        User created = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "integration@test.com", "password", "Integration", "Test"))
                .retrieve()
                .body(User.class);

        assertNotNull(created);
        Long userId = created.getId();
        assertNotNull(userId);

        // READ
        User fetched = restClient.get()
                .uri("/" + userId)
                .retrieve()
                .body(User.class);

        assertNotNull(fetched);
        assertEquals("Integration", fetched.getFirstName());
        assertEquals("integration@test.com", fetched.getEmail());

        // UPDATE
        User updated = restClient.put()
                .uri("/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "updated@test.com", "newpass", "Updated", "Integration"))
                .retrieve()
                .body(User.class);

        assertNotNull(updated);
        assertEquals("Updated", updated.getFirstName());
        assertEquals("updated@test.com", updated.getEmail());

        // DELETE
        restClient.delete()
                .uri("/" + userId)
                .retrieve()
                .toBodilessEntity();

        // VERIFY DELETED
        assertThrows(HttpClientErrorException.NotFound.class, () ->
                restClient.get()
                        .uri("/" + userId)
                        .retrieve()
                        .body(User.class));

        assertEquals(0, userRepository.count());
    }
}

