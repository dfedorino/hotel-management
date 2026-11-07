package ru.hotel.bookingservice.demo;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.bookingservice.domain.Booking;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.repository.BookingRepository;
import ru.hotel.bookingservice.repository.UserRepository;

/**
 * Loads demo accounts (admin, alice, bob; password {@value #DEMO_PASSWORD}) and confirmed bookings 1..3 on an empty
 * database. Rooms, dates and request ids match the reservations seeded by hotel-service, so both services start
 * consistent: bookings 1 and 3 belong to alice, booking 2 to bob.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "booking.demo-data.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    static final String DEMO_PASSWORD = "demo-password";

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Demo data skipped: database is not empty");
            return;
        }
        user("admin", Role.ADMIN);
        User alice = user("alice", Role.USER);
        User bob = user("bob", Role.USER);

        LocalDate today = LocalDate.now(clock);
        confirmedBooking(alice, 1L, today.plusDays(7), today.plusDays(10), 1);
        confirmedBooking(bob, 6L, today.plusDays(3), today.plusDays(5), 2);
        confirmedBooking(alice, 2L, today.plusDays(14), today.plusDays(21), 3);
        log.info("Demo data loaded: {} users, {} bookings", userRepository.count(), bookingRepository.count());
    }

    private User user(String username, Role role) {
        return userRepository.save(new User(username, passwordEncoder.encode(DEMO_PASSWORD), role));
    }

    private void confirmedBooking(User user, Long roomId, LocalDate start, LocalDate end, int number) {
        Booking booking = new Booking(user.getId(), roomId, start, end, "demo-booking-" + number, clock.instant());
        booking.confirm();
        bookingRepository.save(booking);
    }
}
