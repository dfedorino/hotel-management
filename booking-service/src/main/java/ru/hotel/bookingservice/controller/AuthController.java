package ru.hotel.bookingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.hotel.bookingapi.api.AuthApi;
import ru.hotel.bookingapi.model.CredentialsRequest;
import ru.hotel.bookingapi.model.TokenResponse;
import ru.hotel.bookingservice.service.AuthService;

/** Public endpoints: registration and login. */
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public TokenResponse register(CredentialsRequest request) {
        return authService.register(request);
    }

    @Override
    public TokenResponse authenticate(CredentialsRequest request) {
        return authService.authenticate(request);
    }
}
