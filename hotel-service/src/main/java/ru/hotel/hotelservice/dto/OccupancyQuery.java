package ru.hotel.hotelservice.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Period [startDate, endDate) for the occupancy report, optionally limited to one hotel.
 */
public record OccupancyQuery(
        Long hotelId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    @AssertTrue(message = "startDate must be before endDate")
    public boolean isPeriodValid() {
        return startDate == null || endDate == null || startDate.isBefore(endDate);
    }
}
