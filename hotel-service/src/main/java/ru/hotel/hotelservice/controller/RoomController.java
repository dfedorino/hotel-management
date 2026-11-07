package ru.hotel.hotelservice.controller;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import ru.hotel.hotelapi.api.RoomsApi;
import ru.hotel.hotelapi.model.RoomRequest;
import ru.hotel.hotelapi.model.RoomResponse;
import ru.hotel.hotelservice.dto.RoomSearch;
import ru.hotel.hotelservice.service.RoomService;

@RestController
@RequiredArgsConstructor
public class RoomController implements RoomsApi {

    private final RoomService roomService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse createRoom(RoomRequest request) {
        return roomService.create(request);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<RoomResponse> findFreeRooms(Long hotelId, LocalDate startDate, LocalDate endDate) {
        return roomService.findFree(new RoomSearch(hotelId, startDate, endDate));
    }

    @Override
    // INTERNAL: Booking Service picks a room on behalf of the user when autoSelect is requested.
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'INTERNAL')")
    public List<RoomResponse> findRecommendedRooms(Long hotelId, LocalDate startDate, LocalDate endDate) {
        return roomService.findRecommended(new RoomSearch(hotelId, startDate, endDate));
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public RoomResponse getRoom(Long id) {
        return roomService.findById(id);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        return roomService.update(id, request);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRoom(Long id) {
        roomService.delete(id);
    }
}
