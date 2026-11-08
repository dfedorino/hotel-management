package ru.hotel.bookingservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hotel.bookingservice.domain.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByRequestId(String requestId);

    /** Owner-scoped lookup: another user's booking is indistinguishable from a missing one. */
    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserId(Long userId);

    /** Not cancelled stays of the user sharing at least one night with [start, end). */
    @Query("""
            select b from Booking b
            where b.userId = :userId and b.status <> ru.hotel.bookingservice.domain.BookingStatus.CANCELLED
              and b.startDate < :end and b.endDate > :start
            order by b.startDate""")
    List<Booking> findActiveOverlapping(
            @Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
