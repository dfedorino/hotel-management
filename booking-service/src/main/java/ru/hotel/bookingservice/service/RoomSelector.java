package ru.hotel.bookingservice.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.hotel.bookingapi.model.BookingRequest;
import ru.hotel.bookingservice.exception.BadRequestException;
import ru.hotel.bookingservice.exception.ConflictException;
import ru.hotel.bookingservice.exception.HotelErrors;

/**
 * Rooms a booking may take, in the order to try them: the requested room, or with {@code autoSelect} up to
 * {@value #AUTO_SELECT_ATTEMPTS} least booked free rooms (times_booked ascending, then id) so that load spreads evenly.
 */
@Component
@RequiredArgsConstructor
public class RoomSelector {

    static final int AUTO_SELECT_ATTEMPTS = 3;

    private final HotelAvailability hotel;

    public List<Long> roomsToTry(BookingRequest request) {
        if (Boolean.TRUE.equals(request.getAutoSelect())) {
            return leastBookedRooms(request);
        }
        if (request.getRoomId() == null) {
            throw new BadRequestException("roomId is required unless autoSelect is true");
        }
        return List.of(request.getRoomId());
    }

    private List<Long> leastBookedRooms(BookingRequest request) {
        List<RoomCandidate> candidates;
        try {
            candidates = hotel.recommendRooms(request.getHotelId(), request.getStartDate(), request.getEndDate());
        } catch (RuntimeException failure) {
            throw HotelErrors.translate(failure);
        }
        if (candidates.isEmpty()) {
            throw new ConflictException("No free rooms for the requested dates");
        }
        return candidates.stream()
                .limit(AUTO_SELECT_ATTEMPTS)
                .map(RoomCandidate::id)
                .toList();
    }
}
