package ru.hotel.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
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

@SpringBootTest
@AutoConfigureMockMvc
class GatewayRoutingIntegrationTest {

    @RegisterExtension
    static final WireMockExtension BOOKING = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @RegisterExtension
    static final WireMockExtension HOTEL = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void backends(DynamicPropertyRegistry registry) {
        registry.add("gateway.routes.booking-service-uri", BOOKING::baseUrl);
        registry.add("gateway.routes.hotel-service-uri", HOTEL::baseUrl);
        registry.add("security.jwt.secret", () -> GatewayTestTokens.SECRET);
    }

    @BeforeEach
    void backendsAnswer() {
        BOOKING.stubFor(WireMock.post(anyUrl()).willReturn(aResponse().withStatus(201)));
        BOOKING.stubFor(WireMock.get(anyUrl()).willReturn(aResponse().withStatus(200)));
        HOTEL.stubFor(WireMock.get(anyUrl()).willReturn(aResponse().withStatus(200)));
        HOTEL.stubFor(WireMock.post(anyUrl()).willReturn(aResponse().withStatus(200)));
    }

    @Test
    void registrationIsPublicAndReachesBookingServiceWithoutPrefix() throws Exception {
        // when / then
        mockMvc.perform(post("/api/bookings/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
        BOOKING.verify(postRequestedFor(urlPathEqualTo("/user/register")));
    }

    @Test
    void bookingPathsAreStrippedOfTheGatewayPrefixAndKeepTheToken() throws Exception {
        // given
        String token = GatewayTestTokens.valid();

        // when / then
        mockMvc.perform(get("/api/bookings/bookings")
                        .param("page", "0")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        BOOKING.verify(getRequestedFor(urlPathEqualTo("/bookings"))
                .withQueryParam("page", equalTo("0"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(bearer(token))));
    }

    @Test
    void hotelAndRoomPathsReachHotelServiceUnchanged() throws Exception {
        // given
        String token = GatewayTestTokens.valid();

        // when / then
        for (String path : List.of("/api/hotels", "/api/hotels/1", "/api/rooms", "/api/rooms/recommend")) {
            mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk());
            HOTEL.verify(getRequestedFor(urlPathEqualTo(path)));
        }
    }

    @Test
    void internalSagaEndpointsAreNotPublished() throws Exception {
        // given
        String token = GatewayTestTokens.valid();

        // when / then
        for (String path : List.of("/api/rooms/5/confirm-availability", "/api/rooms/5/release")) {
            mockMvc.perform(post(path).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isNotFound());
        }
        HOTEL.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void requestsWithoutValidTokenStopAtTheGateway() throws Exception {
        // given
        String foreign = GatewayTestTokens.signedWithForeignSecret();
        String expired = GatewayTestTokens.expired();

        // when / then
        mockMvc.perform(get("/api/hotels")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/hotels").header(HttpHeaders.AUTHORIZATION, bearer(foreign)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/hotels"));
        mockMvc.perform(get("/api/bookings/bookings").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().isUnauthorized());
        HOTEL.verify(0, anyRequestedFor(anyUrl()));
        BOOKING.verify(0, anyRequestedFor(anyUrl()));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
