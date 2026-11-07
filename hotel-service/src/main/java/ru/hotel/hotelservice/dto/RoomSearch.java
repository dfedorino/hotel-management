package ru.hotel.hotelservice.dto;

import java.time.LocalDate;

/**
 * Filter for free rooms; dates are optional but must come together, endDate is the check-out day.
 */
public record RoomSearch(Long hotelId, LocalDate startDate, LocalDate endDate) {

    public boolean hasDates() {
        return startDate != null && endDate != null;
    }
}
