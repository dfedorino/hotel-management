package ru.hotel.bookingservice.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hotel.bookingapi.model.RecommendedRoom;
import ru.hotel.bookingservice.exception.HotelErrors;

/**
 * Takes free rooms from Hotel Service and orders them here as well, so the order of the answer is guaranteed by this
 * service whatever Hotel Service returns.
 */
@Service
@RequiredArgsConstructor
public class RoomRecommendationServiceImpl implements RoomRecommendationService {

    private static final Comparator<RoomCandidate> LEAST_BOOKED_FIRST =
            Comparator.comparingInt(RoomCandidate::timesBooked).thenComparing(RoomCandidate::id);

    private final HotelAvailability hotel;
    private final Clock clock;

    @Override
    public List<RecommendedRoom> recommend(Long hotelId, LocalDate start, LocalDate end) {
        StayDates.requireValid(start, end, clock);
        List<RoomCandidate> rooms;
        try {
            rooms = hotel.recommendRooms(hotelId, start, end);
        } catch (RuntimeException failure) {
            throw HotelErrors.translate(failure);
        }
        return rooms.stream()
                .sorted(LEAST_BOOKED_FIRST)
                .map(room -> new RecommendedRoom(room.id(), room.hotelId(), room.number(), room.timesBooked()))
                .toList();
    }
}
