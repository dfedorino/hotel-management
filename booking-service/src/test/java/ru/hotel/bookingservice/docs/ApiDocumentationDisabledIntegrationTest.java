package ru.hotel.bookingservice.docs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.security.TokenIssuer;

@SpringBootTest(properties = "booking.api-docs.enabled=false")
@AutoConfigureMockMvc
class ApiDocumentationDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    @ParameterizedTest
    @ValueSource(strings = {"/openapi/booking-service.yaml", "/swagger-ui/index.html", "/v3/api-docs/swagger-config"})
    void documentationIsNotPublicWhenSwitchedOff(String path) throws Exception {
        // when / then
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/openapi/booking-service.yaml", "/swagger-ui/index.html", "/v3/api-docs/swagger-config"})
    void documentationDoesNotExistWhenSwitchedOff(String path) throws Exception {
        // given
        String adminToken =
                tokenIssuer.issue(new User("root", "hash", Role.ADMIN)).getAccessToken();

        // when / then
        mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
