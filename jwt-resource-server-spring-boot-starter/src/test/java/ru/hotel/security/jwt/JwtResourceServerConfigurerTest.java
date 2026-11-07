package ru.hotel.security.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        classes = JwtResourceServerConfigurerTest.SecuredApplication.class,
        properties = "security.jwt.secret=" + JwtTestTokens.SECRET)
@AutoConfigureMockMvc
class JwtResourceServerConfigurerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingTokenGets401AsErrorJson() throws Exception {
        // when / then
        mockMvc.perform(get("/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/admin"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void wrongRoleGets403AsErrorJson() throws Exception {
        // when / then
        mockMvc.perform(get("/admin").header(HttpHeaders.AUTHORIZATION, bearer("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void matchingRoleGetsThroughAndPostNeedsNoCsrfToken() throws Exception {
        // when / then
        mockMvc.perform(get("/admin").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/admin").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void servicesKeepTheirOwnPublicPaths() throws Exception {
        // when / then
        mockMvc.perform(get("/public")).andExpect(status().isOk());
    }

    private static String bearer(String role) {
        return "Bearer "
                + JwtTestTokens.sign(
                        JwtTestTokens.SECRET,
                        "alice",
                        List.of(role),
                        Instant.now().plus(Duration.ofHours(1)));
    }

    @SpringBootApplication
    static class SecuredApplication {

        @Bean
        @SuppressWarnings("PMD.SignatureDeclareThrowsException") // HttpSecurity.build() declares Exception
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.with(new JwtResourceServerConfigurer(), configurer -> {})
                    .authorizeHttpRequests(auth -> auth.requestMatchers("/public")
                            .permitAll()
                            .anyRequest()
                            .authenticated())
                    .build();
        }

        @RestController
        static class SecuredController {

            @GetMapping("/public")
            String open() {
                return "ok";
            }

            @GetMapping("/admin")
            @PreAuthorize("hasRole('ADMIN')")
            String read() {
                return "ok";
            }

            @PostMapping("/admin")
            @PreAuthorize("hasRole('ADMIN')")
            String write() {
                return "ok";
            }
        }
    }
}
