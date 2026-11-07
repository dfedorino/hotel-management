package ru.hotel.bookingservice.mapper;

import org.mapstruct.Mapper;
import ru.hotel.bookingapi.model.UserResponse;
import ru.hotel.bookingservice.domain.User;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);
}
