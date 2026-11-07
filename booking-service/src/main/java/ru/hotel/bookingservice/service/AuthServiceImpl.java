package ru.hotel.bookingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.bookingapi.model.CredentialsRequest;
import ru.hotel.bookingapi.model.TokenResponse;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.exception.ConflictException;
import ru.hotel.bookingservice.exception.InvalidCredentialsException;
import ru.hotel.bookingservice.repository.UserRepository;
import ru.hotel.bookingservice.security.TokenIssuer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;

    @Override
    @Transactional
    public TokenResponse register(CredentialsRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username " + request.getUsername() + " is already taken");
        }
        User user = userRepository.save(
                new User(request.getUsername(), passwordEncoder.encode(request.getPassword()), Role.USER));
        log.info("userId={} registered", user.getId());
        return tokenIssuer.issue(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse authenticate(CredentialsRequest request) {
        User user = userRepository
                .findByUsername(request.getUsername())
                .filter(found -> passwordEncoder.matches(request.getPassword(), found.getPassword()))
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));
        return tokenIssuer.issue(user);
    }
}
