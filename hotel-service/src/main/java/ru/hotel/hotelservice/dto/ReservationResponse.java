package ru.hotel.hotelservice.dto;

import java.time.LocalDate;
import ru.hotel.hotelservice.domain.ReservationStatus;

public record ReservationResponse(
        Long id,
        Long roomId,
        Long bookingId,
        String requestId,
        LocalDate startDate,
        LocalDate endDate,
        ReservationStatus status) {
}
