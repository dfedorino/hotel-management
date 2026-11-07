package ru.hotel.hotelservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hotel.hotelservice.domain.RoomReservation;

public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {

    Optional<RoomReservation> findByRequestId(String requestId);

    @Query("""
            select rr from RoomReservation rr
            join fetch rr.room r
            where (:hotelId is null or r.hotel.id = :hotelId)
              and rr.status = ru.hotel.hotelservice.domain.ReservationStatus.ACTIVE
              and rr.startDate < :end
              and rr.endDate > :start
            """)
    List<RoomReservation> findActiveOverlapping(
            @Param("hotelId") Long hotelId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            select count(rr) > 0 from RoomReservation rr
            where rr.room.id = :roomId
              and rr.status = ru.hotel.hotelservice.domain.ReservationStatus.ACTIVE
              and rr.startDate < :end
              and rr.endDate > :start
            """)
    boolean existsActiveOverlapping(
            @Param("roomId") Long roomId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
