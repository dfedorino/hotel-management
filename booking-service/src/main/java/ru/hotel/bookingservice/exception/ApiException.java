package ru.hotel.bookingservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Business failure that maps to one HTTP status; subclasses name the reason. */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
