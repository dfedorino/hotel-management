package ru.hotel.hotelapi.error;

import ru.hotel.hotelapi.model.ErrorResponse;

public class HotelServiceBadRequestException extends HotelServiceClientException {

    public HotelServiceBadRequestException(ErrorResponse error) {
        super(400, error);
    }
}
