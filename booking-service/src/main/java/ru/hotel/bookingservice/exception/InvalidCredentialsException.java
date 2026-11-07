package ru.hotel.bookingservice.exception;

import org.springframework.http.HttpStatus;

/** Same answer for an unknown user and a wrong password, so usernames cannot be probed. */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
