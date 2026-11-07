package ru.hotel.bookingservice.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hotel.bookingservice.domain.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByRequestId(String requestId);
}
