package ru.hotel.hotelservice.controller;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<RoomResponse> createRoom(RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(request));
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<RoomResponse>> findFreeRooms(Long hotelId, LocalDate startDate, LocalDate endDate) {
        return ResponseEntity.ok(roomService.findFree(new RoomSearch(hotelId, startDate, endDate)));
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<RoomResponse>> findRecommendedRooms(
            Long hotelId, LocalDate startDate, LocalDate endDate) {
        return ResponseEntity.ok(roomService.findRecommended(new RoomSearch(hotelId, startDate, endDate)));
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<RoomResponse> getRoom(Long id) {
        return ResponseEntity.ok(roomService.findById(id));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> updateRoom(Long id, RoomRequest request) {
        return ResponseEntity.ok(roomService.update(id, request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoom(Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
