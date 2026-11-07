package ru.hotel.bookingservice.service;

import java.time.LocalDate;
import java.util.List;
import ru.hotel.bookingapi.model.RecommendedRoom;

/** Rooms to suggest for a stay, so that bookings spread evenly over the rooms. */
public interface RoomRecommendationService {

    /** Free rooms for [start, end), times_booked ascending, then id. */
    List<RecommendedRoom> recommend(Long hotelId, LocalDate start, LocalDate end);
}
