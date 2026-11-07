package ru.hotel.hotelservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless resource server: every request carries an HS256 JWT issued by Booking Service. Endpoint-level role checks
 * live in {@code @PreAuthorize} on controllers.
 */
@Configuration
// Class proxies keep @RestController visible on controllers that implement generated API interfaces.
@EnableMethodSecurity(proxyTargetClass = true)
@EnableConfigurationProperties(JwtProperties.class)
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
            HttpSecurity http,
            SecurityErrorHandler errorHandler,
            @Value("${hotel.api-docs.enabled}") boolean apiDocsEnabled)
            throws Exception {
        // CSRF needs credentials the browser attaches by itself (cookies, sessions). Here the only credential is a
        // bearer JWT in the Authorization header, which a browser never sends implicitly, and no session exists.
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HEALTH_PATH).permitAll();
                    if (apiDocsEnabled) {
                        auth.requestMatchers(API_DOCS_PATHS).permitAll();
                        auth.requestMatchers(GENERATED_SPEC_PATHS).denyAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(
                        oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                                .authenticationEntryPoint(errorHandler)
                                .accessDeniedHandler(errorHandler))
                .exceptionHandling(
                        errors -> errors.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        SecretKeySpec key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public SecurityErrorHandler securityErrorHandler(ObjectMapper objectMapper) {
        return new SecurityErrorHandler(objectMapper);
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
