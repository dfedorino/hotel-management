package ru.hotel.hotelservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import ru.hotel.hotelapi.api.RoomAvailabilityApi;
import ru.hotel.hotelapi.model.AvailabilityRequest;
import ru.hotel.hotelapi.model.ReleaseRequest;
import ru.hotel.hotelapi.model.ReservationResponse;
import ru.hotel.hotelservice.service.RoomAvailabilityService;

/** Internal saga endpoints called by Booking Service; not exposed through the gateway. */
@RestController
@RequiredArgsConstructor
public class RoomAvailabilityController implements RoomAvailabilityApi {

    private final RoomAvailabilityService availabilityService;

    @Override
    @PreAuthorize("hasRole('INTERNAL')")
    public ReservationResponse confirmAvailability(Long id, AvailabilityRequest request) {
        return availabilityService.confirm(id, request);
    }

    @Override
    @PreAuthorize("hasRole('INTERNAL')")
    public void releaseRoom(Long id, ReleaseRequest request) {
        availabilityService.release(id, request);
    }
}
