package ru.hotel.bookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import ru.hotel.bookingservice.domain.Booking;
import ru.hotel.bookingservice.domain.BookingStatus;
import ru.hotel.bookingservice.domain.Role;
import ru.hotel.bookingservice.domain.User;
import ru.hotel.bookingservice.repository.BookingRepository;
import ru.hotel.bookingservice.repository.UserRepository;

@DataJpaTest
@Import({BookingStore.class, BookingStoreTest.FixedClock.class})
class BookingStoreTest {

    private static final Instant NOW = Instant.parse("2027-01-01T10:00:00Z");
    private static final LocalDate START = LocalDate.of(2027, 1, 10);
    private static final LocalDate END = LocalDate.of(2027, 1, 15);

    @Autowired
    private BookingStore store;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = userRepository.save(new User("alice", "hash", Role.USER)).getId();
    }

    @Test
    void createPendingStoresBookingWithCreationTime() {
        // when
        Booking booking = store.createPending(userId, 5L, START, END, "req-1");

        // then
        Booking stored = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(stored.getCreatedAt()).isEqualTo(NOW);
        assertThat(store.findByRequestId("req-1")).contains(stored);
    }

    @Test
    void secondBookingWithSameRequestIdIsRejectedByTheDatabase() {
        // given
        store.createPending(userId, 5L, START, END, "req-1");

        // when / then
        assertThatThrownBy(() -> store.createPending(userId, 5L, START, END, "req-1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void transitionsArePersisted() {
        // given
        Long id = store.createPending(userId, 5L, START, END, "req-1").getId();

        // when
        store.markConfirmed(id);

        // then
        assertThat(bookingRepository.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        store.markCancelled(id);
        assertThat(bookingRepository.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @TestConfiguration
    static class FixedClock {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
