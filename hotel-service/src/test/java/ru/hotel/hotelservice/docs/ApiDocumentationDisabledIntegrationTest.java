package ru.hotel.hotelservice.docs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.hotel.hotelservice.security.JwtTokenFixtures;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"hotel.api-docs.enabled=false", "security.jwt.secret=" + JwtTokenFixtures.SECRET})
class ApiDocumentationDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/openapi/hotel-service.yaml", "/swagger-ui/index.html", "/v3/api-docs/swagger-config"})
    void documentationIsNotPublicWhenSwitchedOff(String path) throws Exception {
        // when / then
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/openapi/hotel-service.yaml", "/swagger-ui/index.html", "/v3/api-docs/swagger-config"})
    void documentationDoesNotExistWhenSwitchedOff(String path) throws Exception {
        // when / then
        mockMvc.perform(get(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTokenFixtures.token("root", "ADMIN")))
                .andExpect(status().isNotFound());
    }
}
