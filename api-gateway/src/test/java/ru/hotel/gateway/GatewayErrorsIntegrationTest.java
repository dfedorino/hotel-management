package ru.hotel.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Errors produced by the gateway itself come in the same JSON shape as the services' ErrorResponse. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayErrorsIntegrationTest {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {};

    @Autowired
    private TestRestTemplate rest;

    @DynamicPropertySource
    static void backends(DynamicPropertyRegistry registry) {
        // nothing listens on port 1: the booking route points at a service that is down
        registry.add("gateway.routes.booking-service-uri", () -> "http://127.0.0.1:1");
        registry.add("security.jwt.secret", () -> GatewayTestTokens.SECRET);
    }

    @Test
    void unknownRouteAnswers404InErrorFormat() {
        // when
        ResponseEntity<Map<String, Object>> response = get("/api/unknown");

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertErrorBody(response.getBody(), 404, "/api/unknown");
    }

    @Test
    void unreachableServiceAnswers503InErrorFormat() {
        // when
        ResponseEntity<Map<String, Object>> response = get("/api/bookings");

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertErrorBody(response.getBody(), 503, "/api/bookings");
        assertThat(response.getBody()).containsEntry("message", "Service is unavailable, try again later");
    }

    private ResponseEntity<Map<String, Object>> get(String path) {
        RequestEntity<Void> request = RequestEntity.get(path)
                .headers(headers -> headers.setBearerAuth(GatewayTestTokens.valid()))
                .build();
        return rest.exchange(request, JSON);
    }

    private static void assertErrorBody(Map<String, Object> body, int status, String path) {
        assertThat(body).containsOnlyKeys("timestamp", "status", "error", "message", "path");
        assertThat(body).containsEntry("status", status).containsEntry("path", path);
        assertThat(body.get("message")).asString().isNotBlank();
    }
}
