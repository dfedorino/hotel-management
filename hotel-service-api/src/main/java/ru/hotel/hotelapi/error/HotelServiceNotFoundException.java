package ru.hotel.hotelapi.error;

import ru.hotel.hotelapi.model.ErrorResponse;

public class HotelServiceNotFoundException extends HotelServiceClientException {

    public HotelServiceNotFoundException(ErrorResponse error) {
        super(404, error);
    }
}
