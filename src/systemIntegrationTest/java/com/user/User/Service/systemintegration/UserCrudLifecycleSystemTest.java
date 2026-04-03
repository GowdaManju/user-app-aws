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
 * System Integration Test: Full end-to-end CRUD lifecycle.
 * Tests the complete user journey — create → read → update → delete
 * with the real HTTP server, database, and all layers wired together.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserCrudLifecycleSystemTest {

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

    // ---- Complete CRUD lifecycle ----

    @Test
    @Order(1)
    void fullLifecycle_CreateReadUpdateDelete_ShouldWorkEndToEnd() {
        // 1. CREATE
        User created = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "mn3118110@gmail.com", "password", "Manjunath", "Gowda"))
                .retrieve()
                .body(User.class);

        assertNotNull(created);
        assertNotNull(created.getId());
        Long userId = created.getId();
        assertEquals("mn3118110@gmail.com", created.getEmail());
        assertEquals("Manjunath", created.getFirstName());
        assertEquals("Gowda", created.getLastName());

        // 2. READ — verify persisted
        User fetched = restClient.get()
                .uri("/" + userId)
                .retrieve()
                .body(User.class);

        assertNotNull(fetched);
        assertEquals(userId, fetched.getId());
        assertEquals("mn3118110@gmail.com", fetched.getEmail());

        // 3. UPDATE
        User updated = restClient.put()
                .uri("/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "updated@gmail.com", "newpassword", "Manju Updated", "G"))
                .retrieve()
                .body(User.class);

        assertNotNull(updated);
        assertEquals("Manju Updated", updated.getFirstName());
        assertEquals("G", updated.getLastName());
        assertEquals("updated@gmail.com", updated.getEmail());

        // 4. READ — verify update persisted
        User fetchedAfterUpdate = restClient.get()
                .uri("/" + userId)
                .retrieve()
                .body(User.class);

        assertEquals("Manju Updated", fetchedAfterUpdate.getFirstName());
        assertEquals("updated@gmail.com", fetchedAfterUpdate.getEmail());

        // 5. DELETE
        restClient.delete()
                .uri("/" + userId)
                .retrieve()
                .toBodilessEntity();

        // 6. READ — verify deleted
        assertThrows(HttpClientErrorException.NotFound.class, () ->
                restClient.get()
                        .uri("/" + userId)
                        .retrieve()
                        .body(User.class));

        // 7. Verify DB is clean
        assertFalse(userRepository.findById(userId).isPresent());
    }

    // ---- Multiple users lifecycle ----

    @Test
    @Order(2)
    void multipleUsers_CreateAndListAndDeleteAll_ShouldWorkCorrectly() {
        // Create 3 users
        User user1 = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "user1@test.com", "pass1", "User", "One"))
                .retrieve()
                .body(User.class);

        User user2 = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "user2@test.com", "pass2", "User", "Two"))
                .retrieve()
                .body(User.class);

        User user3 = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "user3@test.com", "pass3", "User", "Three"))
                .retrieve()
                .body(User.class);

        // List all — should return 3
        User[] allUsers = restClient.get()
                .retrieve()
                .body(User[].class);

        assertNotNull(allUsers);
        assertEquals(3, allUsers.length);

        // Delete user2
        restClient.delete()
                .uri("/" + user2.getId())
                .retrieve()
                .toBodilessEntity();

        // List all — should return 2
        User[] remainingUsers = restClient.get()
                .retrieve()
                .body(User[].class);

        assertNotNull(remainingUsers);
        assertEquals(2, remainingUsers.length);

        // Verify user2 is gone
        assertThrows(HttpClientErrorException.NotFound.class, () ->
                restClient.get()
                        .uri("/" + user2.getId())
                        .retrieve()
                        .body(User.class));

        // Verify user1 and user3 still exist
        User fetchedUser1 = restClient.get()
                .uri("/" + user1.getId())
                .retrieve()
                .body(User.class);
        assertEquals("User", fetchedUser1.getFirstName());
        assertEquals("One", fetchedUser1.getLastName());

        User fetchedUser3 = restClient.get()
                .uri("/" + user3.getId())
                .retrieve()
                .body(User.class);
        assertEquals("User", fetchedUser3.getFirstName());
        assertEquals("Three", fetchedUser3.getLastName());
    }

    // ---- Update then delete flow ----

    @Test
    @Order(3)
    void updateThenDelete_ShouldWorkCorrectly() {
        // Create
        User created = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "original@test.com", "pass", "Original", "Name"))
                .retrieve()
                .body(User.class);

        Long userId = created.getId();

        // Update multiple times
        restClient.put()
                .uri("/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "update1@test.com", "pass1", "Update", "One"))
                .retrieve()
                .body(User.class);

        User finalUpdate = restClient.put()
                .uri("/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "final@test.com", "finalpass", "Final", "Update"))
                .retrieve()
                .body(User.class);

        assertEquals("Final", finalUpdate.getFirstName());
        assertEquals("final@test.com", finalUpdate.getEmail());

        // Delete
        restClient.delete()
                .uri("/" + userId)
                .retrieve()
                .toBodilessEntity();

        // Verify gone
        assertEquals(0, userRepository.count());
    }
}

