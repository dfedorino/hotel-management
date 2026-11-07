package ru.hotel.bookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.hotel.bookingapi.model.UserCreateRequest;
import ru.hotel.bookingapi.model.UserResponse;
import ru.hotel.bookingapi.model.UserUpdateRequest;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.exception.ConflictException;
import ru.hotel.bookingservice.exception.NotFoundException;
import ru.hotel.bookingservice.mapper.UserMapper;
import ru.hotel.bookingservice.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder, Mappers.getMapper(UserMapper.class));
    }

    @Test
    void createStoresUserWithRequestedRole() {
        // given
        given(userRepository.existsByUsername("bob")).willReturn(false);
        given(passwordEncoder.encode("secret-pass")).willReturn("hash");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(3L);
            return user;
        });

        // when
        UserResponse response =
                userService.create(new UserCreateRequest("bob", "secret-pass", ru.hotel.bookingapi.model.Role.ADMIN));

        // then
        assertThat(response).isEqualTo(new UserResponse(3L, "bob", ru.hotel.bookingapi.model.Role.ADMIN));
    }

    @Test
    void updateChangesOnlyGivenFieldsAndReencodesPassword() {
        // given
        User user = new User("bob", "old-hash", Role.USER);
        user.setId(3L);
        given(userRepository.findById(3L)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("new-secret")).willReturn("new-hash");
        UserUpdateRequest request = new UserUpdateRequest().password("new-secret");

        // when
        UserResponse response = userService.update(3L, request);

        // then
        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(response).isEqualTo(new UserResponse(3L, "bob", ru.hotel.bookingapi.model.Role.USER));
    }

    @Test
    void updateRejectsUsernameOfAnotherUser() {
        // given
        User user = new User("bob", "hash", Role.USER);
        user.setId(3L);
        given(userRepository.findById(3L)).willReturn(Optional.of(user));
        given(userRepository.existsByUsernameAndIdNot("alice", 3L)).willReturn(true);
        UserUpdateRequest request = new UserUpdateRequest().username("alice");

        // when / then
        assertThatThrownBy(() -> userService.update(3L, request)).isInstanceOf(ConflictException.class);
        assertThat(user.getUsername()).isEqualTo("bob");
    }

    @Test
    void deleteFailsForMissingUser() {
        // given
        given(userRepository.existsById(42L)).willReturn(false);

        // when / then
        assertThatThrownBy(() -> userService.delete(42L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User 42 not found");
        verify(userRepository, never()).deleteById(any());
    }
}
