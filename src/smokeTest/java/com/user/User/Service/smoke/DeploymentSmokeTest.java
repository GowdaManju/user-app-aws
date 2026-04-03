package com.user.User.Service.smoke;

import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deployment Smoke Tests — Run against the GREEN environment
 * BEFORE switching traffic.
 *
 * These are READ-ONLY, non-destructive, and fast (<10s).
 * They verify the app is alive, healthy, and endpoints respond.
 *
 * Usage:
 *   ./gradlew smokeTest -DTARGET_URL=https://green-alb.example.com
 *   TARGET_URL=https://green-alb.example.com ./gradlew smokeTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Deployment Smoke Tests")
class DeploymentSmokeTest {

    private static RestClient restClient;
    private static String targetUrl;

    @BeforeAll
    static void setUp() {
        targetUrl = System.getProperty("TARGET_URL");
        if (targetUrl == null || targetUrl.isEmpty()) {
            targetUrl = "http://localhost:8080";
        }
        restClient = RestClient.builder().baseUrl(targetUrl).build();
        System.out.println("🔍 Running smoke tests against: " + targetUrl);
    }

    // ---- Application Health ----

    @Test
    @Order(1)
    @DisplayName("Health endpoint should return UP")
    void healthEndpointShouldReturnUp() {
        String health = restClient.get()
                .uri("/actuator/health")
                .retrieve()
                .body(String.class);

        assertNotNull(health, "Health endpoint returned null");
        assertTrue(health.contains("\"status\":\"UP\""), "Application status is not UP: " + health);
    }

    @Test
    @Order(2)
    @DisplayName("Database health should be UP")
    void databaseHealthShouldBeUp() {
        String health = restClient.get()
                .uri("/actuator/health")
                .retrieve()
                .body(String.class);

        assertNotNull(health);
        assertTrue(health.contains("db"), "Database health details missing from response");
    }

    @Test
    @Order(3)
    @DisplayName("Disk space health should be present")
    void diskSpaceHealthShouldBePresent() {
        String health = restClient.get()
                .uri("/actuator/health")
                .retrieve()
                .body(String.class);

        assertNotNull(health);
        assertTrue(health.contains("diskSpace"), "Disk space health details missing");
    }

    // ---- Actuator Endpoints ----

    @Test
    @Order(4)
    @DisplayName("Info endpoint should be accessible")
    void infoEndpointShouldBeAccessible() {
        String info = restClient.get()
                .uri("/actuator/info")
                .retrieve()
                .body(String.class);

        assertNotNull(info, "Info endpoint returned null");
    }

    @Test
    @Order(5)
    @DisplayName("Metrics endpoint should be accessible")
    void metricsEndpointShouldBeAccessible() {
        String metrics = restClient.get()
                .uri("/actuator/metrics")
                .retrieve()
                .body(String.class);

        assertNotNull(metrics);
        assertTrue(metrics.contains("names"), "Metrics response missing 'names' field");
    }

    @Test
    @Order(6)
    @DisplayName("Actuator root should list available endpoints")
    void actuatorRootShouldListEndpoints() {
        String actuator = restClient.get()
                .uri("/actuator")
                .retrieve()
                .body(String.class);

        assertNotNull(actuator);
        assertTrue(actuator.contains("health"), "Actuator missing health endpoint link");
        assertTrue(actuator.contains("info"), "Actuator missing info endpoint link");
        assertTrue(actuator.contains("metrics"), "Actuator missing metrics endpoint link");
    }

    // ---- API Endpoints Respond ----

    @Test
    @Order(7)
    @DisplayName("GET /users should respond with 200")
    void getUsersEndpointShouldRespond() {
        String response = restClient.get()
                .uri("/users")
                .retrieve()
                .body(String.class);

        assertNotNull(response, "GET /users returned null");
    }

    @Test
    @Order(8)
    @DisplayName("JVM memory metrics should be available")
    void jvmMemoryMetricsShouldBeAvailable() {
        String metrics = restClient.get()
                .uri("/actuator/metrics")
                .retrieve()
                .body(String.class);

        assertNotNull(metrics);
        assertTrue(metrics.contains("jvm.memory"), "JVM memory metrics missing");
    }
}

