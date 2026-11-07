package ru.hotel.hotelservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request to hold a room slot for a booking; {@code requestId} makes repeated deliveries idempotent.
 */
public record AvailabilityRequest(
        @NotBlank @Size(max = 64) String requestId,
        @NotNull Long bookingId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {

    @JsonIgnore
    @AssertTrue(message = "startDate must be before endDate")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || startDate.isBefore(endDate);
    }
}
