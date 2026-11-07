package ru.hotel.hotelservice.docs;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.hotel.hotelservice.security.TestTokens;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"hotel.api-docs.enabled=true", "security.jwt.secret=" + TestTokens.SECRET})
class ApiDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contractIsServedWithoutToken() throws Exception {
        // when / then
        mockMvc.perform(get("/openapi/hotel-service.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("title: hotel-service")));
    }

    @Test
    void swaggerUiShowsTheContractFile() throws Exception {
        // when / then
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/openapi/hotel-service.yaml"));
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void specDerivedFromCodeIsNotPublished() throws Exception {
        // when / then
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.token("root", "ADMIN")))
                .andExpect(status().isForbidden());
    }
}
