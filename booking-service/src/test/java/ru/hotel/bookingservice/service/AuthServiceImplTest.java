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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.hotel.bookingapi.model.CredentialsRequest;
import ru.hotel.bookingapi.model.TokenResponse;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.exception.ConflictException;
import ru.hotel.bookingservice.exception.InvalidCredentialsException;
import ru.hotel.bookingservice.repository.UserRepository;
import ru.hotel.bookingservice.security.TokenIssuer;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final TokenResponse TOKEN = new TokenResponse("jwt", "Bearer", 3600L);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenIssuer tokenIssuer;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, tokenIssuer);
    }

    @Test
    void registerStoresEncodedPasswordWithUserRoleAndIssuesToken() {
        // given
        given(userRepository.existsByUsername("alice")).willReturn(false);
        given(passwordEncoder.encode("secret-pass")).willReturn("hash");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(tokenIssuer.issue(any(User.class))).willReturn(TOKEN);

        // when
        TokenResponse response = authService.register(new CredentialsRequest("alice", "secret-pass"));

        // then
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("alice");
        assertThat(saved.getValue().getPassword()).isEqualTo("hash");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(response).isEqualTo(TOKEN);
    }

    @Test
    void registerRejectsTakenUsername() {
        // given
        given(userRepository.existsByUsername("alice")).willReturn(true);
        CredentialsRequest request = new CredentialsRequest("alice", "secret-pass");

        // when / then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Username alice is already taken");
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticateIssuesTokenForMatchingPassword() {
        // given
        User user = new User("alice", "hash", Role.USER);
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("secret-pass", "hash")).willReturn(true);
        given(tokenIssuer.issue(user)).willReturn(TOKEN);

        // when
        TokenResponse response = authService.authenticate(new CredentialsRequest("alice", "secret-pass"));

        // then
        assertThat(response).isEqualTo(TOKEN);
    }

    @Test
    void authenticateGivesSameAnswerForUnknownUserAndWrongPassword() {
        // given
        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(new User("alice", "hash", Role.USER)));
        given(passwordEncoder.matches("wrong-pass", "hash")).willReturn(false);
        CredentialsRequest unknown = new CredentialsRequest("ghost", "secret-pass");
        CredentialsRequest wrongPassword = new CredentialsRequest("alice", "wrong-pass");

        // when / then
        assertThatThrownBy(() -> authService.authenticate(unknown))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
        assertThatThrownBy(() -> authService.authenticate(wrongPassword))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }
}
