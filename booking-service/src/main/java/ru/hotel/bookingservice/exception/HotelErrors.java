package ru.hotel.bookingservice.exception;

import ru.hotel.hotelapi.error.HotelServiceClientException;

/** Translates failures of Hotel Service calls into this service's API errors. */
public final class HotelErrors {

    private HotelErrors() {}

    /** A rejection keeps its status (404, 409, otherwise 400); anything else means Hotel Service did not answer. */
    public static ApiException translate(RuntimeException failure) {
        if (failure instanceof HotelServiceClientException rejected) {
            return switch (rejected.getStatus()) {
                case 404 -> new NotFoundException(rejected.getMessage(), rejected);
                case 409 -> new ConflictException(rejected.getMessage(), rejected);
                default -> new BadRequestException(rejected.getMessage(), rejected);
            };
        }
        return new ServiceUnavailableException("Hotel Service is unavailable, try again later", failure);
    }
}
