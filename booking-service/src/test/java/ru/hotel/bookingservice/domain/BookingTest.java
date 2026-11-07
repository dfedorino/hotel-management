package ru.hotel.bookingservice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BookingTest {

    private static final LocalDate START = LocalDate.of(2027, 1, 10);
    private static final LocalDate END = LocalDate.of(2027, 1, 15);

    @Test
    void newBookingIsPending() {
        // when
        Booking booking = booking();

        // then
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void pendingBookingCanBeConfirmedAndThenCancelled() {
        // given
        Booking booking = booking();

        // when
        booking.confirm();
        boolean cancelled = booking.cancel();

        // then
        assertThat(cancelled).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancellingTwiceChangesNothing() {
        // given
        Booking booking = booking();
        booking.cancel();

        // when
        boolean cancelledAgain = booking.cancel();

        // then
        assertThat(cancelledAgain).isFalse();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancelledBookingCannotBeConfirmed() {
        // given
        Booking booking = booking();
        booking.cancel();

        // when / then
        assertThatThrownBy(booking::confirm)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    private static Booking booking() {
        return new Booking(1L, 5L, START, END, "req-1", Instant.parse("2027-01-01T10:00:00Z"));
    }
}
