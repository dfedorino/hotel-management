package ru.hotel.hotelservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import ru.hotel.hotelapi.api.RoomAvailabilityApi;
import ru.hotel.hotelapi.model.AvailabilityRequest;
import ru.hotel.hotelapi.model.ReleaseRequest;
import ru.hotel.hotelapi.model.ReservationResponse;
import ru.hotel.hotelservice.service.RoomAvailabilityService;

/**
 * Internal saga endpoints called by Booking Service; not exposed through the gateway.
 */
@RestController
@RequiredArgsConstructor
public class RoomAvailabilityController implements RoomAvailabilityApi {

    private final RoomAvailabilityService availabilityService;

    @Override
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<ReservationResponse> confirmAvailability(Long id, AvailabilityRequest request) {
        return ResponseEntity.ok(availabilityService.confirm(id, request));
    }

    @Override
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<Void> releaseRoom(Long id, ReleaseRequest request) {
        availabilityService.release(id, request);
        return ResponseEntity.noContent().build();
    }
}
