package com.user.User.Service.systemintegration;

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

/**
 * System Integration Test: Error handling and edge cases.
 * Tests how the system behaves under invalid inputs, not-found scenarios,
 * and boundary conditions across all layers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserErrorHandlingSystemTest {

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

    // ---- GET non-existent user ----

    @Test
    @Order(1)
    void getUser_WithNonExistentId_ShouldReturn404() {
        assertThrows(HttpClientErrorException.NotFound.class, () ->
                restClient.get()
                        .uri("/999")
                        .retrieve()
                        .body(User.class));
    }

    // ---- UPDATE non-existent user ----

    @Test
    @Order(2)
    void updateUser_WithNonExistentId_ShouldReturn404() {
        assertThrows(HttpClientErrorException.NotFound.class, () ->
                restClient.put()
                        .uri("/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new User(null, "ghost@test.com", "pass", "Ghost", "User"))
                        .retrieve()
                        .body(User.class));
    }

    // ---- DELETE non-existent user ----

    @Test
    @Order(3)
    void deleteUser_WithNonExistentId_ShouldReturn404() {
        assertThrows(HttpClientErrorException.NotFound.class, () ->
                restClient.delete()
                        .uri("/999")
                        .retrieve()
                        .toBodilessEntity());
    }

    // ---- DELETE already deleted user ----

    @Test
    @Order(4)
    void deleteUser_ThenDeleteAgain_ShouldReturn404() {
        User created = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "temp@test.com", "pass", "Temp", "User"))
                .retrieve()
                .body(User.class);

        // First delete — should succeed
        restClient.delete()
                .uri("/" + created.getId())
                .retrieve()
                .toBodilessEntity();

        // Second delete — should 404
        assertThrows(HttpClientErrorException.NotFound.class, () ->
                restClient.delete()
                        .uri("/" + created.getId())
                        .retrieve()
                        .toBodilessEntity());
    }

    // ---- UPDATE after DELETE ----

    @Test
    @Order(5)
    void updateUser_AfterDelete_ShouldReturn404() {
        User created = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "temp@test.com", "pass", "Temp", "User"))
                .retrieve()
                .body(User.class);

        restClient.delete()
                .uri("/" + created.getId())
                .retrieve()
                .toBodilessEntity();

        assertThrows(HttpClientErrorException.NotFound.class, () ->
                restClient.put()
                        .uri("/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new User(null, "updated@test.com", "pass", "Updated", "User"))
                        .retrieve()
                        .body(User.class));
    }

    // ---- GET all when empty ----

    @Test
    @Order(6)
    void getAllUsers_WhenDatabaseEmpty_ShouldReturnEmptyArray() {
        User[] users = restClient.get()
                .retrieve()
                .body(User[].class);

        assertNotNull(users);
        assertEquals(0, users.length);
    }

    // ---- Database consistency after errors ----

    @Test
    @Order(7)
    void databaseConsistency_AfterFailedOperations_ShouldRemainIntact() {
        // Create a user
        User created = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "stable@test.com", "pass", "Stable", "User"))
                .retrieve()
                .body(User.class);

        // Attempt various failed operations
        try {
            restClient.get().uri("/999").retrieve().body(User.class);
        } catch (HttpClientErrorException ignored) {}

        try {
            restClient.put().uri("/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new User(null, "x@x.com", "p", "X", "X"))
                    .retrieve().body(User.class);
        } catch (HttpClientErrorException ignored) {}

        try {
            restClient.delete().uri("/999").retrieve().toBodilessEntity();
        } catch (HttpClientErrorException ignored) {}

        // Original user should still be intact
        User fetched = restClient.get()
                .uri("/" + created.getId())
                .retrieve()
                .body(User.class);

        assertNotNull(fetched);
        assertEquals("Stable", fetched.getFirstName());
        assertEquals("stable@test.com", fetched.getEmail());
        assertEquals(1, userRepository.count());
    }
}

