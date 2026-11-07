package ru.hotel.bookingservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import ru.hotel.security.jwt.JwtResourceServerConfigurer;

/** Registration and login are public; everything else needs a token, roles are checked by {@code @PreAuthorize}. */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_POSTS = {"/api/bookings/users/register", "/api/bookings/users/auth"};
    private static final String HEALTH_PATH = "/actuator/health";

    /** Exactly what Swagger UI needs: the contract file, the UI assets and the UI config. */
    private static final String[] API_DOCS_PATHS = {
        "/openapi/booking-service.yaml", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/swagger-config"
    };

    /** Spec that springdoc derives from code; the contract file is published instead. */
    private static final String[] GENERATED_SPEC_PATHS = {"/v3/api-docs", "/v3/api-docs/**"};

    @Bean
    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // HttpSecurity.build() declares Exception
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Value("${booking.api-docs.enabled}") boolean apiDocsEnabled) throws Exception {
        return http.with(new JwtResourceServerConfigurer(), configurer -> {})
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.POST, PUBLIC_POSTS).permitAll();
                    auth.requestMatchers(HEALTH_PATH).permitAll();
                    if (apiDocsEnabled) {
                        auth.requestMatchers(API_DOCS_PATHS).permitAll();
                        auth.requestMatchers(GENERATED_SPEC_PATHS).denyAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
