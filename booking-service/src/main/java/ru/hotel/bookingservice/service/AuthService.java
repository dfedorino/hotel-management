package ru.hotel.bookingservice.service;

import ru.hotel.bookingapi.model.CredentialsRequest;
import ru.hotel.bookingapi.model.TokenResponse;

public interface AuthService {

    /** Creates a USER account and logs it in. */
    TokenResponse register(CredentialsRequest request);

    TokenResponse authenticate(CredentialsRequest request);
}
