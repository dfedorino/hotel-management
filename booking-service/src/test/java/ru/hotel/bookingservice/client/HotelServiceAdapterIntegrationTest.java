package ru.hotel.bookingservice.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.hotel.bookingservice.service.HotelAvailability;
import ru.hotel.hotelapi.error.HotelServiceConflictException;

@SpringBootTest
@AutoConfigureObservability
class HotelServiceAdapterIntegrationTest {

    private static final String CONFIRM_PATH = "/api/rooms/5/confirm-availability";
    private static final String RELEASE_PATH = "/api/rooms/5/release";
    private static final LocalDate START = LocalDate.of(2027, 1, 10);
    private static final LocalDate END = LocalDate.of(2027, 1, 15);
    private static final String RESERVATION = """
            {"id": 1, "roomId": 5, "bookingId": 100, "requestId": "req-1",
             "startDate": "2027-01-10", "endDate": "2027-01-15", "status": "ACTIVE"}
            """;

    @RegisterExtension
    static final WireMockExtension HOTEL = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private HotelAvailability hotelAvailability;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private JwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void hotelServiceLocation(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.hotel-service[0].uri", HOTEL::baseUrl);
        registry.add("spring.cloud.openfeign.client.config.default.read-timeout", () -> "300");
        registry.add("resilience4j.retry.instances.hotel-service.wait-duration", () -> "10ms");
    }

    @BeforeEach
    void closeCircuit() {
        circuitBreakerRegistry.circuitBreaker(HotelServiceAdapter.HOTEL_SERVICE).reset();
    }

    @Test
    void confirmSendsRequestWithInternalServiceToken() {
        // given
        HOTEL.stubFor(post(CONFIRM_PATH).willReturn(json(200, RESERVATION)));

        // when
        hotelAvailability.confirm(5L, 100L, "req-1", START, END);

        // then
        HOTEL.verify(postRequestedFor(urlPathEqualTo(CONFIRM_PATH))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(matchingJsonPath("$.requestId", equalTo("req-1")))
                .withRequestBody(matchingJsonPath("$.bookingId", equalTo("100"))));
        String token = HOTEL.getAllServeEvents()
                .getFirst()
                .getRequest()
                .getHeader(HttpHeaders.AUTHORIZATION)
                .substring("Bearer ".length());
        assertThat(jwtDecoder.decode(token).getClaimAsStringList("roles")).containsExactly("INTERNAL");
    }

    @Test
    void callsCarryTraceContextForCorrelatedLogs() {
        // given
        HOTEL.stubFor(post(CONFIRM_PATH).willReturn(json(200, RESERVATION)));

        // when
        hotelAvailability.confirm(5L, 100L, "req-1", START, END);

        // then
        HOTEL.verify(postRequestedFor(urlPathEqualTo(CONFIRM_PATH))
                .withHeader("traceparent", matching("00-[0-9a-f]{32}-[0-9a-f]{16}-0[01]")));
    }

    @Test
    void confirmRetriesServerErrorsAndSucceeds() {
        // given
        HOTEL.stubFor(post(CONFIRM_PATH)
                .inScenario("flaky")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second"));
        HOTEL.stubFor(post(CONFIRM_PATH)
                .inScenario("flaky")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("healthy"));
        HOTEL.stubFor(post(CONFIRM_PATH)
                .inScenario("flaky")
                .whenScenarioStateIs("healthy")
                .willReturn(json(200, RESERVATION)));

        // when
        hotelAvailability.confirm(5L, 100L, "req-1", START, END);

        // then
        HOTEL.verify(3, postRequestedFor(urlPathEqualTo(CONFIRM_PATH)));
    }

    @Test
    void confirmGivesUpAfterThreeTimeouts() {
        // given
        HOTEL.stubFor(post(CONFIRM_PATH).willReturn(json(200, RESERVATION).withFixedDelay(1_000)));

        // when / then
        assertThatThrownBy(() -> hotelAvailability.confirm(5L, 100L, "req-1", START, END))
                .isInstanceOf(RetryableException.class);
        HOTEL.verify(3, postRequestedFor(urlPathEqualTo(CONFIRM_PATH)));
    }

    @Test
    void conflictIsAnsweredAtOnceWithoutRetry() {
        // given
        HOTEL.stubFor(post(CONFIRM_PATH).willReturn(json(409, error(409, "Room 5 is not available"))));

        // when / then
        assertThatThrownBy(() -> hotelAvailability.confirm(5L, 100L, "req-1", START, END))
                .isInstanceOf(HotelServiceConflictException.class)
                .hasMessage("Room 5 is not available");
        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(CONFIRM_PATH)));
    }

    @Test
    void circuitOpensWhenHotelServiceKeepsFailingAndStopsCallingIt() {
        // given
        HOTEL.stubFor(post(CONFIRM_PATH).willReturn(aResponse().withStatus(503)));
        for (int call = 0; call < 2; call++) {
            assertThatThrownBy(() -> hotelAvailability.confirm(5L, 100L, "req-1", START, END));
        }
        int callsBeforeOpen = HOTEL.getAllServeEvents().size();

        // when / then
        assertThatThrownBy(() -> hotelAvailability.confirm(5L, 100L, "req-1", START, END))
                .isInstanceOf(CallNotPermittedException.class);
        assertThat(HOTEL.getAllServeEvents()).hasSize(callsBeforeOpen);
    }

    @Test
    void releaseIsRetriedLikeConfirm() {
        // given
        HOTEL.stubFor(post(RELEASE_PATH)
                .inScenario("release")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(502))
                .willSetStateTo("up"));
        HOTEL.stubFor(post(RELEASE_PATH)
                .inScenario("release")
                .whenScenarioStateIs("up")
                .willReturn(aResponse().withStatus(204)));

        // when
        hotelAvailability.release(5L, "req-1");

        // then
        HOTEL.verify(
                2,
                postRequestedFor(urlPathEqualTo(RELEASE_PATH))
                        .withRequestBody(matchingJsonPath("$.requestId", equalTo("req-1"))));
    }

    @Test
    void recommendationsKeepHotelServiceOrder() {
        // given
        HOTEL.stubFor(get(urlPathEqualTo("/api/rooms/recommend"))
                .withQueryParam("hotelId", equalTo("1"))
                .withQueryParam("startDate", equalTo("2027-01-10"))
                .withQueryParam("endDate", equalTo("2027-01-15"))
                .willReturn(json(200, """
                        [{"id": 8, "hotelId": 1, "number": "203", "available": true, "timesBooked": 0},
                         {"id": 3, "hotelId": 1, "number": "103", "available": true, "timesBooked": 1}]
                        """)));

        // when
        List<Long> rooms = hotelAvailability.recommendRooms(1L, START, END);

        // then
        assertThat(rooms).containsExactly(8L, 3L);
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(int status, String body) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    private static String error(int status, String message) {
        return """
                {"timestamp": "2027-01-01T10:00:00Z", "status": %d, "error": "Conflict",
                 "message": "%s", "path": "%s"}
                """.formatted(status, message, CONFIRM_PATH);
    }
}
