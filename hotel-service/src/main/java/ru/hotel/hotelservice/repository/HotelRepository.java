package ru.hotel.hotelservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hotel.hotelservice.domain.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {}
