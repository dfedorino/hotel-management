package ru.hotel.bookingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import ru.hotel.bookingapi.api.UsersApi;
import ru.hotel.bookingapi.model.UserCreateRequest;
import ru.hotel.bookingapi.model.UserPage;
import ru.hotel.bookingapi.model.UserResponse;
import ru.hotel.bookingapi.model.UserUpdateRequest;
import ru.hotel.bookingservice.service.UserService;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController implements UsersApi {

    private final UserService userService;

    @Override
    public UserPage listUsers(Integer page, Integer size) {
        return userService.findAll(page, size);
    }

    @Override
    public UserResponse getUser(Long id) {
        return userService.findById(id);
    }

    @Override
    public UserResponse createUser(UserCreateRequest request) {
        return userService.create(request);
    }

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @Override
    public void deleteUser(Long id) {
        userService.delete(id);
    }
}
