package ru.hotel.bookingservice.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.hotel.bookingservice.domain.Booking;
import ru.hotel.bookingservice.domain.BookingStatus;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.repository.BookingRepository;
import ru.hotel.bookingservice.repository.UserRepository;

@DataJpaTest
class DemoDataInitializerTest {

    private static final LocalDate TODAY = LocalDate.of(2027, 3, 1);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private DemoDataInitializer initializer;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        initializer = new DemoDataInitializer(userRepository, bookingRepository, passwordEncoder, clock);
    }

    @Test
    void seedsAdminAndTwoGuestsWithKnownPasswords() {
        // when
        initializer.run(null);

        // then
        assertThat(userRepository.findAll())
                .extracting(User::getUsername, User::getRole)
                .containsExactlyInAnyOrder(
                        tuple("admin", Role.ADMIN), tuple("alice", Role.USER), tuple("bob", Role.USER));
        User alice = userRepository.findByUsername("alice").orElseThrow();
        assertThat(passwordEncoder.matches(DemoDataInitializer.DEMO_PASSWORD, alice.getPassword()))
                .isTrue();
    }

    @Test
    void seedsConfirmedBookingsMatchingHotelServiceDemoReservations() {
        // when
        initializer.run(null);

        // then: same booking ids, rooms, dates and request ids as hotel-service DemoDataInitializer
        List<Booking> bookings = bookingRepository.findAll();
        assertThat(bookings)
                .extracting(Booking::getId, Booking::getRoomId, Booking::getStartDate, Booking::getRequestId)
                .containsExactlyInAnyOrder(
                        tuple(1L, 1L, TODAY.plusDays(7), "demo-booking-1"),
                        tuple(2L, 6L, TODAY.plusDays(3), "demo-booking-2"),
                        tuple(3L, 2L, TODAY.plusDays(14), "demo-booking-3"));
        assertThat(bookings).allMatch(booking -> booking.getStatus() == BookingStatus.CONFIRMED);
    }

    @Test
    void doesNothingWhenUsersAlreadyExist() {
        // given
        userRepository.save(new User("someone", "hash", Role.USER));

        // when
        initializer.run(null);

        // then
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(bookingRepository.count()).isZero();
    }
}
