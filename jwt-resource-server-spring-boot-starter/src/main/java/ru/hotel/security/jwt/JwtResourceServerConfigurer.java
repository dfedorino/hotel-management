package ru.hotel.security.jwt;

import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Turns a filter chain into a stateless JWT resource server: {@code http.with(new JwtResourceServerConfigurer(), c ->
 * {})}, then add the service's own {@code authorizeHttpRequests} rules.
 *
 * <p>CSRF is disabled because the only credential is a bearer token in the Authorization header, which a browser never
 * attaches implicitly, and no session is created.
 */
public class JwtResourceServerConfigurer extends AbstractHttpConfigurer<JwtResourceServerConfigurer, HttpSecurity> {

    @Override
    public void init(HttpSecurity http) throws Exception {
        ApplicationContext context = http.getSharedObject(ApplicationContext.class);
        JwtAuthenticationConverter converter = context.getBean(JwtAuthenticationConverter.class);
        SecurityErrorHandler errorHandler = context.getBean(SecurityErrorHandler.class);

        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(
                        errors -> errors.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler));
    }
}
