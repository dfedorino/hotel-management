package ru.hotel.bookingservice.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.bookingapi.model.UserCreateRequest;
import ru.hotel.bookingapi.model.UserPage;
import ru.hotel.bookingapi.model.UserResponse;
import ru.hotel.bookingapi.model.UserUpdateRequest;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.exception.ConflictException;
import ru.hotel.bookingservice.exception.NotFoundException;
import ru.hotel.bookingservice.mapper.UserMapper;
import ru.hotel.bookingservice.repository.BookingRepository;
import ru.hotel.bookingservice.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Sort BY_ID = Sort.by("id");

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserPage findAll(int page, int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size, BY_ID));
        List<UserResponse> content =
                users.getContent().stream().map(userMapper::toResponse).toList();
        return new UserPage(content, page, size, users.getTotalElements(), users.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(userRepository.findById(id).orElseThrow(() -> notFound(id)));
    }

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw taken(request.getUsername());
        }
        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                Role.valueOf(request.getRole().name()));
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> notFound(id));
        String username = request.getUsername();
        if (username != null) {
            if (userRepository.existsByUsernameAndIdNot(username, id)) {
                throw taken(username);
            }
            user.setUsername(username);
        }
        String password = request.getPassword();
        if (password != null) {
            user.setPassword(passwordEncoder.encode(password));
        }
        ru.hotel.bookingapi.model.Role role = request.getRole();
        if (role != null) {
            user.setRole(Role.valueOf(role.name()));
        }
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw notFound(id);
        }
        // bookings keep the history of the stay, so their owner stays too
        if (bookingRepository.existsByUserId(id)) {
            throw new ConflictException("User " + id + " has bookings and cannot be deleted");
        }
        userRepository.deleteById(id);
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException("User " + id + " not found");
    }

    private static ConflictException taken(String username) {
        return new ConflictException("Username " + username + " is already taken");
    }
}
