package ru.hotel.bookingservice.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hotel.bookingservice.domain.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByRequestId(String requestId);

    /** Owner-scoped lookup: another user's booking is indistinguishable from a missing one. */
    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    Page<Booking> findByUserId(Long userId, Pageable pageable);
}
