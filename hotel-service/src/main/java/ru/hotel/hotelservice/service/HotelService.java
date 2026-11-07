package ru.hotel.hotelservice.service;

import java.util.List;
import ru.hotel.hotelservice.dto.HotelRequest;
import ru.hotel.hotelservice.dto.HotelResponse;

public interface HotelService {

    HotelResponse create(HotelRequest request);

    List<HotelResponse> findAll();

    HotelResponse findById(Long id);

    HotelResponse update(Long id, HotelRequest request);

    void delete(Long id);
}
