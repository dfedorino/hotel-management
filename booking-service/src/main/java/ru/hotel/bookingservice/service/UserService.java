package ru.hotel.bookingservice.service;

import ru.hotel.bookingapi.model.UserCreateRequest;
import ru.hotel.bookingapi.model.UserResponse;
import ru.hotel.bookingapi.model.UserUpdateRequest;

/** User administration for ADMIN. */
public interface UserService {

    UserResponse create(UserCreateRequest request);

    /** Changes only the fields present in the request. */
    UserResponse update(Long id, UserUpdateRequest request);

    void delete(Long id);
}
