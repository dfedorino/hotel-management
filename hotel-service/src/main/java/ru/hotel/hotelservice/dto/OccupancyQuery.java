package ru.hotel.hotelservice.dto;

import java.time.LocalDate;

/**
 * Period [startDate, endDate) for the occupancy report, optionally limited to one hotel.
 */
public record OccupancyQuery(Long hotelId, LocalDate startDate, LocalDate endDate) {
}
