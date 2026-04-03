package com.user.User.Service.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorHealthCheckTest {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/actuator")
                .build();
    }

    @Test
    void healthEndpoint_ShouldReturnUp() {
        String response = restClient.get()
                .uri("/health")
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertTrue(response.contains("\"status\":\"UP\""));
    }

    @Test
    void healthEndpoint_ShouldContainDiskSpaceDetails() {
        String response = restClient.get()
                .uri("/health")
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertTrue(response.contains("diskSpace"));
    }

    @Test
    void healthEndpoint_ShouldContainDbDetails() {
        String response = restClient.get()
                .uri("/health")
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertTrue(response.contains("db"));
    }

    @Test
    void infoEndpoint_ShouldBeAccessible() {
        String response = restClient.get()
                .uri("/info")
                .retrieve()
                .body(String.class);

        assertNotNull(response);
    }

    @Test
    void metricsEndpoint_ShouldBeAccessible() {
        String response = restClient.get()
                .uri("/metrics")
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertTrue(response.contains("names"));
    }
}

