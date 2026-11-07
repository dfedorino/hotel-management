package ru.hotel.hotelservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import ru.hotel.security.jwt.JwtResourceServerConfigurer;

/**
 * Path rules of Hotel Service on top of the shared JWT resource server; role checks per endpoint live in
 * {@code @PreAuthorize} on controllers.
 */
@Configuration
public class SecurityConfig {

    private static final String HEALTH_PATH = "/actuator/health";

    /** Exactly what Swagger UI needs: the contract file, the UI assets and the UI config. */
    private static final String[] API_DOCS_PATHS = {
        "/openapi/hotel-service.yaml", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/swagger-config"
    };

    /** Spec that springdoc derives from code; the contract file is published instead. */
    private static final String[] GENERATED_SPEC_PATHS = {"/v3/api-docs", "/v3/api-docs/**"};

    @Bean
    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // HttpSecurity.build() declares Exception
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Value("${hotel.api-docs.enabled}") boolean apiDocsEnabled) throws Exception {
        return http.with(new JwtResourceServerConfigurer(), configurer -> {})
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HEALTH_PATH).permitAll();
                    if (apiDocsEnabled) {
                        auth.requestMatchers(API_DOCS_PATHS).permitAll();
                        auth.requestMatchers(GENERATED_SPEC_PATHS).denyAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .build();
    }
}
