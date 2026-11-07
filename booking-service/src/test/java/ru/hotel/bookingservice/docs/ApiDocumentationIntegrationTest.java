package ru.hotel.bookingservice.docs;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.security.TokenIssuer;

@SpringBootTest(properties = "booking.api-docs.enabled=true")
@AutoConfigureMockMvc
class ApiDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Test
    void contractIsServedWithoutToken() throws Exception {
        // when / then
        mockMvc.perform(get("/openapi/booking-service.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("title: booking-service")));
    }

    @Test
    void swaggerUiShowsTheContractFile() throws Exception {
        // when / then
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/openapi/booking-service.yaml"));
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void specDerivedFromCodeIsNotPublished() throws Exception {
        // given
        String adminToken =
                tokenIssuer.issue(new User("root", "hash", Role.ADMIN)).getAccessToken();

        // when / then
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }
}
