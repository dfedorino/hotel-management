package ru.hotel.hotelservice.dto;

import java.time.LocalDate;
import java.util.List;

public record OccupancyReport(
        LocalDate startDate,
        LocalDate endDate,
        long periodNights,
        int totalRooms,
        double averageOccupancy,
        List<RoomOccupancy> rooms) {
}
