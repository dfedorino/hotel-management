package ru.hotel.hotelservice.service;

import java.util.List;
import ru.hotel.hotelservice.dto.RoomRequest;
import ru.hotel.hotelservice.dto.RoomResponse;
import ru.hotel.hotelservice.dto.RoomSearch;

public interface RoomService {

    /**
     * Free rooms without special ordering (by id).
     */
    List<RoomResponse> findFree(RoomSearch search);

    /**
     * Free rooms, least booked first, ties broken by id, to spread load evenly.
     */
    List<RoomResponse> findRecommended(RoomSearch search);

    RoomResponse create(RoomRequest request);

    RoomResponse findById(Long id);

    RoomResponse update(Long id, RoomRequest request);

    void delete(Long id);
}
