package ru.hotel.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureObservability
class GatewayTracingIntegrationTest {

    private static final String TRACE_ID = "[0-9a-f]{32}";

    @RegisterExtension
    static final WireMockExtension BOOKING = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private WebTestClient client;

    @DynamicPropertySource
    static void backends(DynamicPropertyRegistry registry) {
        registry.add("gateway.routes.booking-service-uri", BOOKING::baseUrl);
        registry.add("security.jwt.secret", () -> "test-secret-test-secret-test-secret-0123");
    }

    @Test
    void responseCarriesTraceIdThatIsPropagatedDownstream() {
        // given
        BOOKING.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(201)));

        // when
        String traceId = client.post()
                .uri("/api/bookings/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isCreated()
                .returnResult(Void.class)
                .getResponseHeaders()
                .getFirst(TraceIdResponseFilter.TRACE_ID_HEADER);

        // then
        assertThat(traceId).matches(TRACE_ID);
        String traceparent = BOOKING.getAllServeEvents().getFirst().getRequest().getHeader("traceparent");
        assertThat(traceparent).contains(traceId);
    }

    @Test
    void rejectedRequestAlsoCarriesTraceId() {
        // when / then
        client.get()
                .uri("/api/hotels")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectHeader()
                .valueMatches(TraceIdResponseFilter.TRACE_ID_HEADER, TRACE_ID);
    }
}
