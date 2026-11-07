package ru.hotel.bookingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import ru.hotel.bookingapi.api.BookingsApi;
import ru.hotel.bookingapi.model.BookingPage;
import ru.hotel.bookingapi.model.BookingRequest;
import ru.hotel.bookingapi.model.BookingResponse;
import ru.hotel.bookingservice.security.CurrentUser;
import ru.hotel.bookingservice.service.BookingService;

/** Bookings of the authenticated user only. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class BookingController implements BookingsApi {

    private final BookingService bookingService;
    private final CurrentUser currentUser;

    @Override
    public BookingResponse createBooking(BookingRequest request) {
        return bookingService.create(currentUser.id(), request);
    }

    @Override
    public BookingPage listBookings(Integer page, Integer size) {
        return bookingService.findAll(currentUser.id(), page, size);
    }

    @Override
    public BookingResponse getBooking(Long id) {
        return bookingService.findById(currentUser.id(), id);
    }

    @Override
    public void cancelBooking(Long id) {
        bookingService.cancel(currentUser.id(), id);
    }
}
