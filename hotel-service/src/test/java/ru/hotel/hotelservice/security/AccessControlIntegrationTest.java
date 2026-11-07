package ru.hotel.hotelservice.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "security.jwt.secret=" + JwtTokenFixtures.SECRET)
class AccessControlIntegrationTest {

    private static final String HOTEL_BODY = """
            {"name": "Grand", "address": "Main st. 1"}
            """;
    private static final String CONFIRM_BODY = """
            {"requestId": "req-1", "bookingId": 1, "startDate": "2027-01-10", "endDate": "2027-01-15"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requestWithoutTokenGets401WithErrorBody() throws Exception {
        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/hotels"));
    }

    @Test
    void tokenSignedWithForeignSecretGets401() throws Exception {
        mockMvc.perform(withToken(get("/api/hotels"), JwtTokenFixtures.foreignToken("USER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenGets401() throws Exception {
        mockMvc.perform(withToken(get("/api/hotels"), JwtTokenFixtures.expiredToken("USER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownPathGets404WithErrorBody() throws Exception {
        mockMvc.perform(withToken(get("/api/unknown"), JwtTokenFixtures.token("alice", "USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void userCanListHotels() throws Exception {
        mockMvc.perform(withToken(get("/api/hotels"), JwtTokenFixtures.token("alice", "USER")))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotCreateHotel() throws Exception {
        mockMvc.perform(withToken(post("/api/hotels"), JwtTokenFixtures.token("alice", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(HOTEL_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCanCreateHotel() throws Exception {
        mockMvc.perform(withToken(post("/api/hotels"), JwtTokenFixtures.token("root", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(HOTEL_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void userCannotSeeOccupancy() throws Exception {
        mockMvc.perform(withToken(get("/api/rooms/occupancy"), JwtTokenFixtures.token("alice", "USER"))
                        .param("startDate", "2027-01-01")
                        .param("endDate", "2027-01-11"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotCallInternalSagaEndpoints() throws Exception {
        mockMvc.perform(withToken(post("/api/rooms/1/confirm-availability"), JwtTokenFixtures.token("alice", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONFIRM_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(post("/api/rooms/1/release"), JwtTokenFixtures.token("root", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\": \"req-1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalServiceReachesSagaEndpoint() throws Exception {
        mockMvc.perform(withToken(
                                post("/api/rooms/999999/confirm-availability"),
                                JwtTokenFixtures.token("booking-service", "INTERNAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONFIRM_BODY))
                .andExpect(status().isNotFound());
    }

    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
