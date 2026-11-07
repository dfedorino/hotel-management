package ru.hotel.hotelservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.hotel.hotelservice.domain.RoomReservation;
import ru.hotel.hotelapi.model.ReservationResponse;

@Mapper
public interface ReservationMapper {

    @Mapping(target = "roomId", source = "room.id")
    ReservationResponse toResponse(RoomReservation reservation);
}
