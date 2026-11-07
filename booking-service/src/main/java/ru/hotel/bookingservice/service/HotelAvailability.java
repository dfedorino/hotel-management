package ru.hotel.bookingservice.service;

import java.time.LocalDate;
import java.util.List;

/**
 * What the booking saga needs from Hotel Service. Rejections (4xx) surface as
 * {@link ru.hotel.hotelapi.error.HotelServiceClientException}; anything else means Hotel Service could not answer.
 */
public interface HotelAvailability {

    /** Holds the slot; repeating the same requestId has no further effect. */
    void confirm(Long roomId, Long bookingId, String requestId, LocalDate start, LocalDate end);

    /** Compensation: releases the slot held by requestId; a no-op if nothing is held. */
    void release(Long roomId, String requestId);

    /** Free rooms, least booked first. */
    List<RoomCandidate> recommendRooms(Long hotelId, LocalDate start, LocalDate end);
}
