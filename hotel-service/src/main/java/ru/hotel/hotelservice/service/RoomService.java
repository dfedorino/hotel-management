package ru.hotel.hotelservice.service;

import ru.hotel.hotelservice.dto.RoomRequest;
import ru.hotel.hotelservice.dto.RoomResponse;

public interface RoomService {

    RoomResponse create(RoomRequest request);

    RoomResponse findById(Long id);

    RoomResponse update(Long id, RoomRequest request);

    void delete(Long id);
}
