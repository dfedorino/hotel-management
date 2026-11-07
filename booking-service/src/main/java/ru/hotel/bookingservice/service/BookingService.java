package ru.hotel.bookingservice.service;

import ru.hotel.bookingapi.model.BookingPage;
import ru.hotel.bookingapi.model.BookingRequest;
import ru.hotel.bookingapi.model.BookingResponse;

/** Bookings of one user; every method is scoped to {@code userId}. */
public interface BookingService {

    /** Runs the booking saga; a repeated requestId returns the booking created the first time. */
    BookingResponse create(Long userId, BookingRequest request);

    BookingResponse findById(Long userId, Long bookingId);

    BookingPage findAll(Long userId, int page, int size);

    /** Releases the slot in Hotel Service and cancels; cancelling twice is a no-op. */
    void cancel(Long userId, Long bookingId);
}
