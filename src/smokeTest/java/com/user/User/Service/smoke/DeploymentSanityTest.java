package com.user.User.Service.smoke;

import com.user.User.Service.user.User;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deployment Sanity Tests — Run after smoke tests pass,
 * BEFORE switching full traffic to Green.
 *
 * Tests critical CRUD flows against the live environment.
 * IMPORTANT: Creates test data and CLEANS UP after itself.
 *
 * Usage:
 *   ./gradlew smokeTest -DTARGET_URL=https://green-alb.example.com
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Deployment Sanity Tests")
class DeploymentSanityTest {

    private static RestClient restClient;
    private static String targetUrl;
    private static Long createdUserId;

    @BeforeAll
    static void setUp() {
        targetUrl = System.getProperty("TARGET_URL");
        if (targetUrl == null || targetUrl.isEmpty()) {
            targetUrl = "http://localhost:8080";
        }
        restClient = RestClient.builder()
                .baseUrl(targetUrl + "/users")
                .build();
        System.out.println("🔍 Running sanity tests against: " + targetUrl);
    }

    @AfterAll
    static void cleanup() {
        // Safety net — delete the test user if it still exists
        if (createdUserId != null) {
            try {
                restClient.delete()
                        .uri("/" + createdUserId)
                        .retrieve()
                        .toBodilessEntity();
                System.out.println("🧹 Cleaned up test user: " + createdUserId);
            } catch (Exception ignored) {
                // Already deleted or doesn't exist — that's fine
            }
        }
    }

    // ---- CREATE ----

    @Test
    @Order(1)
    @DisplayName("Should create a test user")
    void shouldCreateTestUser() {
        User user = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "smoke-test@deployment.com", "smokepass", "Smoke", "Test"))
                .retrieve()
                .body(User.class);

        assertNotNull(user, "Created user is null");
        assertNotNull(user.getId(), "Created user ID is null");
        assertEquals("smoke-test@deployment.com", user.getEmail());
        assertEquals("Smoke", user.getFirstName());
        assertEquals("Test", user.getLastName());

        createdUserId = user.getId();
    }

    // ---- READ ----

    @Test
    @Order(2)
    @DisplayName("Should read the created test user")
    void shouldReadCreatedUser() {
        assertNotNull(createdUserId, "No user was created in previous test");

        User user = restClient.get()
                .uri("/" + createdUserId)
                .retrieve()
                .body(User.class);

        assertNotNull(user, "Fetched user is null");
        assertEquals(createdUserId, user.getId());
        assertEquals("smoke-test@deployment.com", user.getEmail());
        assertEquals("Smoke", user.getFirstName());
    }

    // ---- UPDATE ----

    @Test
    @Order(3)
    @DisplayName("Should update the test user")
    void shouldUpdateTestUser() {
        assertNotNull(createdUserId, "No user was created in previous test");

        User updated = restClient.put()
                .uri("/" + createdUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new User(null, "smoke-updated@deployment.com", "newpass", "SmokeUpdated", "TestUpdated"))
                .retrieve()
                .body(User.class);

        assertNotNull(updated, "Updated user is null");
        assertEquals("smoke-updated@deployment.com", updated.getEmail());
        assertEquals("SmokeUpdated", updated.getFirstName());
        assertEquals("TestUpdated", updated.getLastName());
    }

    // ---- READ after UPDATE ----

    @Test
    @Order(4)
    @DisplayName("Should read updated user and verify changes persisted")
    void shouldReadUpdatedUser() {
        assertNotNull(createdUserId, "No user was created in previous test");

        User user = restClient.get()
                .uri("/" + createdUserId)
                .retrieve()
                .body(User.class);

        assertNotNull(user);
        assertEquals("smoke-updated@deployment.com", user.getEmail());
        assertEquals("SmokeUpdated", user.getFirstName());
    }

    // ---- DELETE (cleanup) ----

    @Test
    @Order(5)
    @DisplayName("Should delete the test user (cleanup)")
    void shouldDeleteTestUser() {
        assertNotNull(createdUserId, "No user was created in previous test");

        restClient.delete()
                .uri("/" + createdUserId)
                .retrieve()
                .toBodilessEntity();

        // Mark as cleaned up so @AfterAll doesn't try again
        createdUserId = null;
    }

    // ---- GET all should still work after cleanup ----

    @Test
    @Order(6)
    @DisplayName("GET /users should respond after cleanup")
    void shouldListUsersAfterCleanup() {
        String response = restClient.get()
                .retrieve()
                .body(String.class);

        assertNotNull(response, "GET /users returned null after cleanup");
    }
}

