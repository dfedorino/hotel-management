package ru.hotel.hotelservice.service;

import java.time.LocalDate;
import ru.hotel.hotelservice.exception.BadRequestException;

/**
 * Validation of [start, end) stay periods shared by search, slot locking and statistics.
 */
final class Periods {

    private Periods() {
    }

    static void requireStartBeforeEnd(LocalDate start, LocalDate end) {
        if (!start.isBefore(end)) {
            throw new BadRequestException("startDate must be before endDate");
        }
    }
}
