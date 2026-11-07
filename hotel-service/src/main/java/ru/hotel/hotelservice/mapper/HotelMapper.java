package ru.hotel.hotelservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.hotel.hotelservice.domain.Hotel;
import ru.hotel.hotelservice.dto.HotelRequest;
import ru.hotel.hotelservice.dto.HotelResponse;

@Mapper
public interface HotelMapper {

    HotelResponse toResponse(Hotel hotel);

    Hotel toEntity(HotelRequest request);

    @Mapping(target = "id", ignore = true)
    void update(@MappingTarget Hotel hotel, HotelRequest request);
}
