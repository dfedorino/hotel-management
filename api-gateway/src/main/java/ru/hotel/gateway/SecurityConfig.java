package ru.hotel.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import ru.hotel.security.jwt.JwtResourceServerCustomizer;

/**
 * First line of defence: requests without a valid, unexpired token never reach a service. Only authentication is
 * checked here; roles stay with the services, which validate the relayed token again.
 */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_POSTS = {"/api/bookings/users/register", "/api/bookings/users/auth"};
    private static final String HEALTH_PATH = "/actuator/health";

    /** Static browser client (src/main/resources/static); everything it calls under /api still needs a token. */
    private static final String[] WEB_CLIENT_PATHS = {"/", "/index.html", "/ui/**"};

    @Bean
    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // HttpSecurity.build() declares Exception
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtResourceServerCustomizer jwtResourceServer)
            throws Exception {
        return jwtResourceServer
                .customize(http)
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, PUBLIC_POSTS)
                        .permitAll()
                        .requestMatchers(HEALTH_PATH)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, WEB_CLIENT_PATHS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .build();
    }
}
