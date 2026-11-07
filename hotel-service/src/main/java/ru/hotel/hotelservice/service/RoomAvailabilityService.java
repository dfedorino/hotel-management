package ru.hotel.hotelservice.service;

import ru.hotel.hotelapi.model.AvailabilityRequest;
import ru.hotel.hotelapi.model.ReleaseRequest;
import ru.hotel.hotelapi.model.ReservationResponse;

/** Saga participant: holds and releases room slots for Booking Service. */
public interface RoomAvailabilityService {

    /**
     * Holds the slot and counts the booking; a repeated request with the same requestId returns the original
     * reservation without side effects.
     */
    ReservationResponse confirm(Long roomId, AvailabilityRequest request);

    /** Releases the slot held by requestId; releasing an unknown or already released slot is a no-op. */
    void release(Long roomId, ReleaseRequest request);
}
