package ru.hotel.bookingservice.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.bookingservice.domain.Booking;
import ru.hotel.bookingservice.exception.ConflictException;
import ru.hotel.bookingservice.exception.NotFoundException;
import ru.hotel.bookingservice.repository.BookingRepository;
import ru.hotel.bookingservice.repository.UserRepository;

/**
 * Each saga step as its own local transaction, committed before Hotel Service is called, so a failure there never rolls
 * back what this service already recorded.
 */
@Component
@RequiredArgsConstructor
public class BookingStore {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * A user holds at most one stay per night: the user row lock makes concurrent requests of one user check and insert
     * one after another. Flushes at once so a duplicate requestId fails here, not later at commit.
     */
    @Transactional
    public Booking createPending(Long userId, Long roomId, LocalDate start, LocalDate end, String requestId) {
        userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        bookingRepository.findActiveOverlapping(userId, start, end).stream()
                // a redelivery of the same request is not a second stay: the unique requestId rejects it below
                .filter(existing -> !existing.getRequestId().equals(requestId))
                .findFirst()
                .ifPresent(existing -> {
                    throw new ConflictException("You already have a booking from %s to %s overlapping these dates"
                            .formatted(existing.getStartDate(), existing.getEndDate()));
                });
        return bookingRepository.saveAndFlush(new Booking(userId, roomId, start, end, requestId, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Optional<Booking> findByRequestId(String requestId) {
        return bookingRepository.findByRequestId(requestId);
    }

    @Transactional
    public Booking markConfirmed(Long id) {
        Booking booking = get(id);
        booking.confirm();
        return booking;
    }

    /** Automatic selection: the PENDING booking takes the next recommended room under a new slot key. */
    @Transactional
    public Booking moveToRoom(Long id, Long roomId, String slotRequestId) {
        Booking booking = get(id);
        booking.moveTo(roomId, slotRequestId);
        return booking;
    }

    /** No-op for an already cancelled booking. */
    @Transactional
    public Booking markCancelled(Long id) {
        Booking booking = get(id);
        booking.cancel();
        return booking;
    }

    private Booking get(Long id) {
        return bookingRepository
                .findById(id)
                .orElseThrow(() -> new IllegalStateException("Booking " + id + " vanished"));
    }
}
