package ru.hotel.bookingservice.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.hotel.bookingservice.domain.Booking;
import ru.hotel.bookingservice.exception.ApiException;
import ru.hotel.bookingservice.exception.HotelErrors;
import ru.hotel.bookingservice.exception.ServiceUnavailableException;
import ru.hotel.hotelapi.error.HotelServiceClientException;
import ru.hotel.hotelapi.error.HotelServiceConflictException;

/**
 * Second step of the booking saga, run against a PENDING booking already committed by {@link BookingStore}:
 *
 * <ul>
 *   <li>Hotel Service holds the slot: CONFIRMED;
 *   <li>the room is taken (409): with automatic selection the next recommended room is tried under its own slot key,
 *       otherwise CANCELLED;
 *   <li>any other rejection: CANCELLED;
 *   <li>no answer after retries: the slot is released (it may have been held before the answer was lost), CANCELLED.
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSaga {

    private final BookingStore store;
    private final HotelAvailability hotel;

    public Booking holdSlot(Booking pending, List<Long> rooms) {
        Booking booking = pending;
        for (int attempt = 1; ; attempt++) {
            try {
                confirm(booking);
                break;
            } catch (HotelServiceConflictException taken) {
                if (attempt >= rooms.size()) {
                    throw reject(booking, taken);
                }
                booking = moveToNextRoom(booking, rooms.get(attempt), attempt + 1);
            } catch (HotelServiceClientException rejected) {
                throw reject(booking, rejected);
            } catch (RuntimeException failure) {
                compensate(booking, failure);
                throw new ServiceUnavailableException(
                        "Hotel Service is unavailable, the booking was cancelled", failure);
            }
        }
        Booking confirmed = store.markConfirmed(booking.getId());
        log.info("requestId={} roomId={} booking CONFIRMED", booking.getRequestId(), booking.getRoomId());
        return confirmed;
    }

    private void confirm(Booking booking) {
        hotel.confirm(
                booking.getRoomId(),
                booking.getId(),
                booking.getSlotRequestId(),
                booking.getStartDate(),
                booking.getEndDate());
    }

    private Booking moveToNextRoom(Booking booking, Long nextRoomId, int attempt) {
        log.info(
                "requestId={} roomId={} taken meanwhile, trying roomId={}",
                booking.getRequestId(),
                booking.getRoomId(),
                nextRoomId);
        return store.moveToRoom(booking.getId(), nextRoomId, booking.getRequestId() + "#" + attempt);
    }

    private ApiException reject(Booking booking, HotelServiceClientException rejected) {
        store.markCancelled(booking.getId());
        log.info("requestId={} booking CANCELLED: {}", booking.getRequestId(), rejected.getMessage());
        return HotelErrors.translate(rejected);
    }

    private void compensate(Booking booking, RuntimeException failure) {
        log.warn("requestId={} Hotel Service failed: {}, compensating", booking.getRequestId(), failure.toString());
        try {
            hotel.release(booking.getRoomId(), booking.getSlotRequestId());
            log.info("requestId={} compensation done: slot released", booking.getRequestId());
        } catch (RuntimeException releaseFailure) {
            log.error(
                    "requestId={} compensation failed, slot may stay held in Hotel Service: {}",
                    booking.getRequestId(),
                    releaseFailure.toString());
        } finally {
            store.markCancelled(booking.getId());
            log.info("requestId={} booking CANCELLED", booking.getRequestId());
        }
    }
}
