package ru.hotel.hotelapi.error;

import ru.hotel.hotelapi.model.ErrorResponse;

/** Duplicate, overlapping dates or a room out of service. */
public class HotelServiceConflictException extends HotelServiceClientException {

    public HotelServiceConflictException(ErrorResponse error) {
        super(409, error);
    }
}
