package ru.hotel.hotelservice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hotel.hotelservice.domain.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelId(Long hotelId);

    boolean existsByHotelId(Long hotelId);

    boolean existsByHotelIdAndNumber(Long hotelId, String number);

    boolean existsByHotelIdAndNumberAndIdNot(Long hotelId, String number, Long id);
}
