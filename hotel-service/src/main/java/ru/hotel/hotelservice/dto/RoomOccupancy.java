package ru.hotel.hotelservice.dto;

/**
 * Occupancy of one room: {@code occupancyRate} is booked nights divided by nights in the period.
 */
public record RoomOccupancy(
        Long roomId,
        Long hotelId,
        String number,
        int timesBooked,
        long bookedNights,
        double occupancyRate) {
}
