package ru.hotel.hotelservice.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hotel.hotelservice.domain.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelId(Long hotelId);

    boolean existsByHotelId(Long hotelId);

    boolean existsByHotelIdAndNumber(Long hotelId, String number);

    boolean existsByHotelIdAndNumberAndIdNot(Long hotelId, String number, Long id);

    /**
     * Rooms in service, optionally of one hotel.
     */
    @Query("""
            select r from Room r
            where r.available = true
              and (:hotelId is null or r.hotel.id = :hotelId)
            """)
    List<Room> findAvailable(@Param("hotelId") Long hotelId, Sort sort);

    /**
     * Rooms in service with no active reservation overlapping [start, end).
     */
    @Query("""
            select r from Room r
            where r.available = true
              and (:hotelId is null or r.hotel.id = :hotelId)
              and not exists (
                  select 1 from RoomReservation rr
                  where rr.room = r
                    and rr.status = ru.hotel.hotelservice.domain.ReservationStatus.ACTIVE
                    and rr.startDate < :end
                    and rr.endDate > :start)
            """)
    List<Room> findFree(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            Sort sort);
}
