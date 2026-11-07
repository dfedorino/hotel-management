package ru.hotel.bookingservice.security;

import ru.hotel.bookingapi.model.TokenResponse;
import ru.hotel.bookingservice.domain.User;

/** Issues the JWTs every service validates with the shared secret. */
public interface TokenIssuer {

    TokenResponse issue(User user);

    /** Short-lived token with the INTERNAL role for saga calls to Hotel Service. */
    String issueServiceToken();
}
