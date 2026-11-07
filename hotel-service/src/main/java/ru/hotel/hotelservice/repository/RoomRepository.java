package ru.hotel.hotelservice.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hotel.hotelservice.domain.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelId(Long hotelId);

    /**
     * Loads the room with a row lock so that slot changes of one room are serialized.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    boolean existsByHotelId(Long hotelId);

    boolean existsByHotelIdAndNumber(Long hotelId, String number);

    boolean existsByHotelIdAndNumberAndIdNot(Long hotelId, String number, Long id);

    @Query("select r from Room r where :hotelId is null or r.hotel.id = :hotelId")
    List<Room> findAllInHotel(@Param("hotelId") Long hotelId, Sort sort);

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
