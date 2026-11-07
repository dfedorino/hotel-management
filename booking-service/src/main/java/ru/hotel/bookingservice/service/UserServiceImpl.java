package ru.hotel.bookingservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.bookingapi.model.UserCreateRequest;
import ru.hotel.bookingapi.model.UserResponse;
import ru.hotel.bookingapi.model.UserUpdateRequest;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.exception.ConflictException;
import ru.hotel.bookingservice.exception.NotFoundException;
import ru.hotel.bookingservice.mapper.UserMapper;
import ru.hotel.bookingservice.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

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
        userRepository.deleteById(id);
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException("User " + id + " not found");
    }

    private static ConflictException taken(String username) {
        return new ConflictException("Username " + username + " is already taken");
    }
}
