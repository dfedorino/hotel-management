package ru.hotel.hotelservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.hotel.hotelservice.dto.AvailabilityRequest;
import ru.hotel.hotelservice.dto.ReleaseRequest;
import ru.hotel.hotelservice.dto.ReservationResponse;
import ru.hotel.hotelservice.service.RoomAvailabilityService;

/**
 * Internal saga endpoints called by Booking Service; not exposed through the gateway.
 */
@RestController
@RequestMapping("/api/rooms/{id}")
@RequiredArgsConstructor
public class RoomAvailabilityController {

    private final RoomAvailabilityService availabilityService;

    @PreAuthorize("hasRole('INTERNAL')")
    @PostMapping("/confirm-availability")
    public ReservationResponse confirm(@PathVariable Long id, @Valid @RequestBody AvailabilityRequest request) {
        return availabilityService.confirm(id, request);
    }

    @PreAuthorize("hasRole('INTERNAL')")
    @PostMapping("/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable Long id, @Valid @RequestBody ReleaseRequest request) {
        availabilityService.release(id, request);
    }
}
