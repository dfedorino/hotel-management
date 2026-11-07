package ru.hotel.hotelapi.error;

import lombok.Getter;
import ru.hotel.hotelapi.model.ErrorResponse;

/**
 * A 4xx answer of Hotel Service: the request was understood and rejected, so retrying will not help.
 */
@Getter
public abstract class HotelServiceClientException extends RuntimeException {

    private final int status;
    private final ErrorResponse error;

    protected HotelServiceClientException(int status, ErrorResponse error) {
        super(error.getMessage());
        this.status = status;
        this.error = error;
    }
}
