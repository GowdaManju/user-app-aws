package com.user.User.Service.systemintegration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * System Integration Test: Actuator and health check endpoints.
 * Verifies that monitoring endpoints are accessible and return
 * expected data when the full application is running.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorSystemTest {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/actuator")
                .build();
    }

    // ---- Health endpoint ----

    @Test
    void healthEndpoint_ShouldReturnStatusUp() {
        String response = restClient.get()
                .uri("/health")
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertTrue(response.contains("\"status\":\"UP\""));
    }

    @Test
    void healthEndpoint_ShouldContainDatabaseHealth() {
        String response = restClient.get()
                .uri("/health")
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertTrue(response.contains("db"));
    }

    @Test
    void healthEndpoint_ShouldContainDiskSpaceHealth() {
        String response = restClient.get()
                .uri("/health")
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertTrue(response.contains("diskSpace"));
    }

    // ---- Info endpoint ----

    @Test
    void infoEndpoint_ShouldBeAccessible() {
        String response = restClient.get()
                .uri("/info")
                .retrieve()
                .body(String.class);

        assertNotNull(response);
    }

    // ---- Metrics endpoint ----

    @Test
    void metricsEndpoint_ShouldBeAccessible() {
        String response = restClient.get()
                .uri("/metrics")
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertTrue(response.contains("names"));
    }

    @Test
    void metricsEndpoint_ShouldContainJvmMetrics() {
        String response = restClient.get()
                .uri("/metrics")
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertTrue(response.contains("jvm.memory"));
    }

    // ---- Actuator root ----

    @Test
    void actuatorRoot_ShouldListAvailableEndpoints() {
        String response = restClient.get()
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertTrue(response.contains("health"));
        assertTrue(response.contains("info"));
        assertTrue(response.contains("metrics"));
    }
}

