package ru.hotel.bookingservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.hotel.bookingservice.service.HotelAvailability;
import ru.hotel.hotelapi.api.RoomAvailabilityApiClient;
import ru.hotel.hotelapi.api.RoomsApiClient;
import ru.hotel.hotelapi.model.AvailabilityRequest;
import ru.hotel.hotelapi.model.ReleaseRequest;
import ru.hotel.hotelapi.model.RoomResponse;

/**
 * Hotel Service calls behind timeouts (Feign), retries with exponential backoff and a circuit breaker (Resilience4j,
 * instance {@value #HOTEL_SERVICE} in application.yml). Retrying is safe: every call is idempotent by requestId on the
 * Hotel Service side.
 */
@Component
@RequiredArgsConstructor
@Retry(name = HotelServiceAdapter.HOTEL_SERVICE)
@CircuitBreaker(name = HotelServiceAdapter.HOTEL_SERVICE)
public class HotelServiceAdapter implements HotelAvailability {

    static final String HOTEL_SERVICE = "hotel-service";

    private final RoomAvailabilityApiClient roomAvailabilityClient;
    private final RoomsApiClient roomsClient;

    @Override
    public void confirm(Long roomId, Long bookingId, String requestId, LocalDate start, LocalDate end) {
        roomAvailabilityClient.confirmAvailability(roomId, new AvailabilityRequest(requestId, bookingId, start, end));
    }

    @Override
    public void release(Long roomId, String requestId) {
        roomAvailabilityClient.releaseRoom(roomId, new ReleaseRequest(requestId));
    }

    @Override
    public List<Long> recommendRooms(Long hotelId, LocalDate start, LocalDate end) {
        return roomsClient.findRecommendedRooms(hotelId, start, end).stream()
                .map(RoomResponse::getId)
                .toList();
    }
}
