package ru.hotel.hotelservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.hotel.hotelapi.model.RoomResponse;
import ru.hotel.hotelservice.domain.Room;

@Mapper
public interface RoomMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    RoomResponse toResponse(Room room);
}
