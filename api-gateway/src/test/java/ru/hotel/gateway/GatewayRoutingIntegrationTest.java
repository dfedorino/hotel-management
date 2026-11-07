package ru.hotel.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayRoutingIntegrationTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123";
    private static final String FOREIGN_SECRET = "another-secret-another-secret-another-1234";

    @RegisterExtension
    static final WireMockExtension BOOKING = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static final WireMockExtension HOTEL = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private WebTestClient client;

    @DynamicPropertySource
    static void backends(DynamicPropertyRegistry registry) {
        registry.add("gateway.routes.booking-service-uri", BOOKING::baseUrl);
        registry.add("gateway.routes.hotel-service-uri", HOTEL::baseUrl);
        registry.add("security.jwt.secret", () -> SECRET);
    }

    @BeforeEach
    void backendsAnswer() {
        BOOKING.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(201)));
        BOOKING.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));
        HOTEL.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));
        HOTEL.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));
    }

    @Test
    void registrationIsPublicAndReachesBookingServiceWithoutPrefix() {
        // when / then
        client.post()
                .uri("/api/bookings/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isCreated();
        BOOKING.verify(postRequestedFor(urlPathEqualTo("/user/register")));
    }

    @Test
    void bookingPathsAreStrippedOfTheGatewayPrefix() {
        // given
        String token = token(SECRET, Instant.now().plus(Duration.ofHours(1)));

        // when / then
        client.get()
                .uri("/api/bookings/bookings?page=0")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();
        BOOKING.verify(getRequestedFor(urlPathEqualTo("/bookings"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token)));
    }

    @Test
    void hotelAndRoomPathsReachHotelServiceUnchanged() {
        // given
        String token = token(SECRET, Instant.now().plus(Duration.ofHours(1)));

        // when / then
        for (String path : List.of("/api/hotels", "/api/hotels/1", "/api/rooms", "/api/rooms/recommend")) {
            client.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus()
                    .isOk();
            HOTEL.verify(getRequestedFor(urlPathEqualTo(path)));
        }
    }

    @Test
    void internalSagaEndpointsAreNotPublished() {
        // given
        String token = token(SECRET, Instant.now().plus(Duration.ofHours(1)));

        // when / then
        for (String path : List.of("/api/rooms/5/confirm-availability", "/api/rooms/5/release")) {
            client.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus()
                    .isNotFound();
        }
        HOTEL.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void requestsWithoutValidTokenStopAtTheGateway() {
        // given
        String foreign = token(FOREIGN_SECRET, Instant.now().plus(Duration.ofHours(1)));
        String expired = token(SECRET, Instant.now().minus(Duration.ofMinutes(5)));

        // when / then
        client.get().uri("/api/hotels").exchange().expectStatus().isUnauthorized();
        client.get()
                .uri("/api/hotels")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + foreign)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(401)
                .jsonPath("$.path")
                .isEqualTo("/api/hotels");
        client.get()
                .uri("/api/bookings/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired)
                .exchange()
                .expectStatus()
                .isUnauthorized();
        HOTEL.verify(0, anyRequestedFor(anyUrl()));
        BOOKING.verify(0, anyRequestedFor(anyUrl()));
    }

    private static String token(String secret, Instant expiresAt) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("alice")
                .claim("roles", List.of("USER"))
                .issueTime(Date.from(expiresAt.minus(Duration.ofHours(1))))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
        return jwt.serialize();
    }
}
