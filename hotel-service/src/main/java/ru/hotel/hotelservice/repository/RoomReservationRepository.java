package ru.hotel.hotelservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hotel.hotelservice.domain.RoomReservation;

public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {
}
