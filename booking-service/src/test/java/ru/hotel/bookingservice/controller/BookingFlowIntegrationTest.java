package ru.hotel.bookingservice.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.jayway.jsonpath.JsonPath;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.hotel.bookingservice.domain.BookingStatus;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.repository.BookingRepository;
import ru.hotel.bookingservice.repository.UserRepository;
import ru.hotel.bookingservice.security.TokenIssuer;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("PMD.SignatureDeclareThrowsException") // MockMvc.perform() declares Exception
class BookingFlowIntegrationTest {

    private static final String CONFIRM_PATH = "/api/rooms/5/confirm-availability";
    private static final String RELEASE_PATH = "/api/rooms/5/release";
    private static final LocalDate START = LocalDate.now().plusDays(10);
    private static final LocalDate END = START.plusDays(3);

    @RegisterExtension
    static final WireMockExtension HOTEL = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private String aliceToken;
    private String bobToken;

    @DynamicPropertySource
    static void hotelServiceLocation(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.hotel-service[0].uri", HOTEL::baseUrl);
        registry.add("spring.cloud.openfeign.client.config.default.read-timeout", () -> "300");
        registry.add("resilience4j.retry.instances.hotel-service.wait-duration", () -> "10ms");
    }

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry.circuitBreaker("hotel-service").reset();
        aliceToken = tokenFor("alice");
        bobToken = tokenFor("bob");
        HOTEL.stubFor(post(RELEASE_PATH).willReturn(aResponse().withStatus(204)));
    }

    @AfterEach
    void tearDown() {
        bookingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void bookingIsConfirmedWhenHotelHoldsTheSlot() throws Exception {
        // given
        HOTEL.stubFor(post(CONFIRM_PATH).willReturn(reservation()));

        // when
        String body = book(aliceToken, "req-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.roomId").value(5))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        Integer id = JsonPath.read(body, "$.id");
        mockMvc.perform(get("/booking/" + id).header(HttpHeaders.AUTHORIZATION, bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-1"));
        mockMvc.perform(get("/bookings").header(HttpHeaders.AUTHORIZATION, bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
    }

    @Test
    void bookingIsCancelledWhenHotelRejectsTheDates() throws Exception {
        // given
        HOTEL.stubFor(post(CONFIRM_PATH).willReturn(json(409, """
                {"timestamp": "2027-01-01T10:00:00Z", "status": 409, "error": "Conflict",
                 "message": "Room 5 is not available", "path": "/api/rooms/5/confirm-availability"}
                """)));

        // when
        book(aliceToken, "req-1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Room 5 is not available"));

        // then
        assertThat(bookingRepository.findByRequestId("req-1").orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
        HOTEL.verify(0, postRequestedFor(urlPathEqualTo(RELEASE_PATH)));
    }

    @Test
    void bookingIsCompensatedWhenHotelTimesOut() throws Exception {
        // given
        HOTEL.stubFor(post(CONFIRM_PATH).willReturn(reservation().withFixedDelay(1_000)));

        // when
        book(aliceToken, "req-1").andExpect(status().isServiceUnavailable());

        // then
        assertThat(bookingRepository.findByRequestId("req-1").orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
        HOTEL.verify(3, postRequestedFor(urlPathEqualTo(CONFIRM_PATH)));
        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(RELEASE_PATH)));
    }

    @Test
    void redeliveredRequestReturnsTheSameBookingAndHoldsTheSlotOnce() throws Exception {
        // given
        HOTEL.stubFor(post(CONFIRM_PATH).willReturn(reservation()));
        String first = book(aliceToken, "req-1").andReturn().getResponse().getContentAsString();

        // when
        String second = book(aliceToken, "req-1")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        assertThat((Integer) JsonPath.read(second, "$.id")).isEqualTo(JsonPath.read(first, "$.id"));
        assertThat(bookingRepository.count()).isEqualTo(1);
        HOTEL.verify(1, postRequestedFor(urlPathEqualTo(CONFIRM_PATH)));
    }

    @Test
    void usersSeeOnlyTheirOwnBookings() throws Exception {
        // given
        HOTEL.stubFor(post(CONFIRM_PATH).willReturn(reservation()));
        String body = book(aliceToken, "req-1").andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        // when / then
        mockMvc.perform(get("/booking/" + id).header(HttpHeaders.AUTHORIZATION, bearer(bobToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/booking/" + id).header(HttpHeaders.AUTHORIZATION, bearer(bobToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/bookings").header(HttpHeaders.AUTHORIZATION, bearer(bobToken)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void cancellingReleasesTheSlotOnce() throws Exception {
        // given
        HOTEL.stubFor(post(CONFIRM_PATH).willReturn(reservation()));
        String body = book(aliceToken, "req-1").andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        // when
        mockMvc.perform(delete("/booking/" + id).header(HttpHeaders.AUTHORIZATION, bearer(aliceToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/booking/" + id).header(HttpHeaders.AUTHORIZATION, bearer(aliceToken)))
                .andExpect(status().isNoContent());

        // then
        assertThat(bookingRepository.findById(id.longValue()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
        HOTEL.verify(1, postRequestedFor(urlPathMatching(RELEASE_PATH)));
    }

    @Test
    void bookingNeedsToken() throws Exception {
        // when / then
        mockMvc.perform(MockMvcRequestBuilders.post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody("req-1")))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions book(String token, String requestId) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/booking")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(requestId)));
    }

    private String tokenFor(String username) {
        User user = userRepository.save(new User(username, "unused-hash", Role.USER));
        return tokenIssuer.issue(user).getAccessToken();
    }

    private static String bookingBody(String requestId) {
        return """
                {"roomId": 5, "startDate": "%s", "endDate": "%s", "requestId": "%s"}
                """.formatted(START, END, requestId);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder reservation() {
        return json(200, """
                {"id": 1, "roomId": 5, "bookingId": 1, "requestId": "req-1",
                 "startDate": "%s", "endDate": "%s", "status": "ACTIVE"}
                """.formatted(START, END));
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(int status, String body) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }
}
