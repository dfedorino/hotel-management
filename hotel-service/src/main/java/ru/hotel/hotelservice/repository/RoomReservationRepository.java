package ru.hotel.hotelservice.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hotel.hotelservice.domain.RoomReservation;

public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {

    Optional<RoomReservation> findByRequestId(String requestId);

    @Query("""
            select count(rr) > 0 from RoomReservation rr
            where rr.room.id = :roomId
              and rr.status = ru.hotel.hotelservice.domain.ReservationStatus.ACTIVE
              and rr.startDate < :end
              and rr.endDate > :start
            """)
    boolean existsActiveOverlapping(
            @Param("roomId") Long roomId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
