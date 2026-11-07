package ru.hotel.bookingservice.exception;

import org.springframework.http.HttpStatus;

/** Hotel Service did not answer after retries or the circuit is open. */
public class ServiceUnavailableException extends ApiException {

    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
