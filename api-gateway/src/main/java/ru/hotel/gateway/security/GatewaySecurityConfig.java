package ru.hotel.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * First line of defence: requests without a valid, unexpired token never reach a service. Only authentication is
 * checked here; roles stay with the services, which validate the relayed token again. CSRF is off because the only
 * credential is a bearer token that a browser never attaches implicitly.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private static final String[] PUBLIC_POSTS = {"/api/bookings/user/register", "/api/bookings/user/auth"};
    private static final String HEALTH_PATH = "/actuator/health";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ObjectMapper objectMapper) {
        JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(objectMapper);
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.POST, PUBLIC_POSTS)
                        .permitAll()
                        .pathMatchers(HEALTH_PATH)
                        .permitAll()
                        .anyExchange()
                        .authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}).authenticationEntryPoint(entryPoint))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint))
                .build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(JwtSecretProperties properties) {
        SecretKeySpec key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
