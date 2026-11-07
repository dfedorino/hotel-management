package ru.hotel.hotelservice.service;

import java.util.List;
import ru.hotel.hotelapi.model.HotelRequest;
import ru.hotel.hotelapi.model.HotelResponse;

public interface HotelService {

    HotelResponse create(HotelRequest request);

    List<HotelResponse> findAll();

    HotelResponse findById(Long id);

    HotelResponse update(Long id, HotelRequest request);

    void delete(Long id);
}
