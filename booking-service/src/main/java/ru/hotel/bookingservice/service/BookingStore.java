package ru.hotel.bookingservice.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.bookingservice.domain.Booking;
import ru.hotel.bookingservice.repository.BookingRepository;

/**
 * Each saga step as its own local transaction, committed before Hotel Service is called, so a failure there never rolls
 * back what this service already recorded.
 */
@Component
@RequiredArgsConstructor
public class BookingStore {

    private final BookingRepository bookingRepository;
    private final Clock clock;

    /** Flushes at once so a duplicate requestId fails here, not later at commit. */
    @Transactional
    public Booking createPending(Long userId, Long roomId, LocalDate start, LocalDate end, String requestId) {
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
