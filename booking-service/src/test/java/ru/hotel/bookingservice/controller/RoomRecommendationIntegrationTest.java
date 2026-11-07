package ru.hotel.bookingservice.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.security.TokenIssuer;

@SpringBootTest
@AutoConfigureMockMvc
class RoomRecommendationIntegrationTest {

    private static final LocalDate START = LocalDate.now().plusDays(10);
    private static final LocalDate END = START.plusDays(3);

    @RegisterExtension
    static final WireMockExtension HOTEL = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void hotelServiceLocation(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.hotel-service[0].uri", HOTEL::baseUrl);
    }

    @BeforeEach
    void closeCircuit() {
        circuitBreakerRegistry.circuitBreaker("hotel-service").reset();
    }

    @Test
    void userGetsFreeRoomsLeastBookedFirst() throws Exception {
        // given
        HOTEL.stubFor(get(urlPathEqualTo("/api/rooms/recommend"))
                .withQueryParam("startDate", equalTo(START.toString()))
                .withQueryParam("endDate", equalTo(END.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        [{"id": 7, "hotelId": 1, "number": "202", "available": true, "timesBooked": 2},
                         {"id": 3, "hotelId": 1, "number": "103", "available": true, "timesBooked": 0}]
                        """)));
        String token = tokenIssuer.issue(user()).getAccessToken();

        // when / then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/bookings/rooms/recommend")
                        .param("startDate", START.toString())
                        .param("endDate", END.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].timesBooked").value(0))
                .andExpect(jsonPath("$[1].id").value(7));
    }

    @Test
    void periodIsRequired() throws Exception {
        // given
        String token = tokenIssuer.issue(user()).getAccessToken();

        // when / then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/bookings/rooms/recommend")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recommendationsNeedToken() throws Exception {
        // when / then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/bookings/rooms/recommend")
                        .param("startDate", START.toString())
                        .param("endDate", END.toString()))
                .andExpect(status().isUnauthorized());
    }

    private static User user() {
        User user = new User("alice", "hash", Role.USER);
        user.setId(1L);
        return user;
    }
}
