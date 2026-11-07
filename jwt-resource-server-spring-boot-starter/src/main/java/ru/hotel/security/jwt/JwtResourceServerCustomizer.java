package ru.hotel.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Turns a filter chain into a stateless JWT resource server. Inject it and continue with the service's own rules:
 * {@code jwtResourceServer.customize(http).authorizeHttpRequests(...).build()}. Holds no state, so it can shape any
 * number of filter chains.
 *
 * <p>CSRF is disabled because the only credential is a bearer token in the Authorization header, which a browser never
 * attaches implicitly, and no session is created.
 */
@RequiredArgsConstructor
public class JwtResourceServerCustomizer {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final SecurityErrorHandler errorHandler;

    /** HttpSecurity methods declare {@code Exception}, hence the signature. */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public HttpSecurity customize(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(
                        oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                                .authenticationEntryPoint(errorHandler)
                                .accessDeniedHandler(errorHandler))
                .exceptionHandling(
                        errors -> errors.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler));
    }
}
