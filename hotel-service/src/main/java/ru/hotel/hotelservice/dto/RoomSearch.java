package ru.hotel.hotelservice.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filter for free rooms; dates are optional but must come together, endDate is the check-out day.
 */
public record RoomSearch(
        Long hotelId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    public boolean hasDates() {
        return startDate != null && endDate != null;
    }
}
