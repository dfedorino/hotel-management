package ru.hotel.bookingservice.service;

import java.time.Clock;
import java.time.LocalDate;
import ru.hotel.bookingservice.exception.BadRequestException;

/** Validation of a stay [start, end): end is the check-out day, the stay may not begin in the past. */
final class StayDates {

    private StayDates() {}

    static void requireValid(LocalDate start, LocalDate end, Clock clock) {
        if (!start.isBefore(end)) {
            throw new BadRequestException("startDate must be before endDate");
        }
        if (start.isBefore(LocalDate.now(clock))) {
            throw new BadRequestException("startDate must not be in the past");
        }
    }
}
