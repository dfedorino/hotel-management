package ru.hotel.bookingservice.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Id of the authenticated user, taken from the {@code uid} claim of the token this service issued. */
@Component
public class CurrentUser {

    public Long id() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            Object userId = token.getToken().getClaim(JwtTokenIssuer.USER_ID_CLAIM);
            if (userId != null) {
                return Long.valueOf(userId.toString());
            }
        }
        throw new IllegalStateException("Request is not authenticated with a user token");
    }
}
