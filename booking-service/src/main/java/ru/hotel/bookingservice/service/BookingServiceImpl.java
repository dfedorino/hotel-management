package ru.hotel.bookingservice.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.hotel.bookingapi.model.BookingPage;
import ru.hotel.bookingapi.model.BookingRequest;
import ru.hotel.bookingapi.model.BookingResponse;
import ru.hotel.bookingservice.domain.Booking;
import ru.hotel.bookingservice.domain.BookingStatus;
import ru.hotel.bookingservice.exception.ConflictException;
import ru.hotel.bookingservice.exception.HotelErrors;
import ru.hotel.bookingservice.exception.NotFoundException;
import ru.hotel.bookingservice.mapper.BookingMapper;
import ru.hotel.bookingservice.repository.BookingRepository;

/**
 * Bookings of a user. Creating one is a saga of local transactions: the booking is first committed as PENDING (see
 * {@link BookingStore}), then {@link BookingSaga} asks Hotel Service to hold the slot and confirms or compensates. A
 * repeated requestId returns the booking created the first time. All steps are logged with {@code bookingId} in the
 * MDC.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    static final String BOOKING_ID = "bookingId";
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final BookingStore store;
    private final BookingRepository bookingRepository;
    private final RoomSelector roomSelector;
    private final BookingSaga saga;
    private final HotelAvailability hotel;
    private final BookingMapper bookingMapper;
    private final Clock clock;

    @Override
    public BookingResponse create(Long userId, BookingRequest request) {
        StayDates.requireValid(request.getStartDate(), request.getEndDate(), clock);
        String requestId = Optional.ofNullable(request.getRequestId())
                .orElseGet(() -> UUID.randomUUID().toString());
        Optional<Booking> earlier = store.findByRequestId(requestId);
        if (earlier.isPresent()) {
            return replay(earlier.get(), userId);
        }
        List<Long> rooms = roomSelector.roomsToTry(request);
        Booking booking;
        try {
            booking = store.createPending(
                    userId, rooms.getFirst(), request.getStartDate(), request.getEndDate(), requestId);
        } catch (DataIntegrityViolationException duplicate) {
            // a concurrent delivery of the same request won the insert
            return replay(store.findByRequestId(requestId).orElseThrow(() -> duplicate), userId);
        }
        try (MDC.MDCCloseable ignored = MDC.putCloseable(BOOKING_ID, String.valueOf(booking.getId()))) {
            log.info("requestId={} roomId={} booking PENDING", requestId, booking.getRoomId());
            return bookingMapper.toResponse(saga.holdSlot(booking, rooms));
        }
    }

    @Override
    public BookingResponse findById(Long userId, Long bookingId) {
        return bookingMapper.toResponse(getOwn(userId, bookingId));
    }

    @Override
    public BookingPage findAll(Long userId, int page, int size) {
        Page<Booking> bookings = bookingRepository.findByUserId(userId, PageRequest.of(page, size, NEWEST_FIRST));
        List<BookingResponse> content =
                bookings.getContent().stream().map(bookingMapper::toResponse).toList();
        return new BookingPage(content, page, size, bookings.getTotalElements(), bookings.getTotalPages());
    }

    @Override
    public void cancel(Long userId, Long bookingId) {
        Booking booking = getOwn(userId, bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }
        try (MDC.MDCCloseable ignored = MDC.putCloseable(BOOKING_ID, String.valueOf(bookingId))) {
            try {
                hotel.release(booking.getRoomId(), booking.getSlotRequestId());
            } catch (RuntimeException failure) {
                // the booking stays as it was, so the user can simply retry the cancellation
                log.warn("requestId={} release failed: {}", booking.getRequestId(), failure.toString());
                throw HotelErrors.translate(failure);
            }
            store.markCancelled(bookingId);
            log.info("requestId={} booking CANCELLED by user", booking.getRequestId());
        }
    }

    private BookingResponse replay(Booking earlier, Long userId) {
        if (!earlier.getUserId().equals(userId)) {
            throw new ConflictException("requestId " + earlier.getRequestId() + " is already used");
        }
        log.info(
                "bookingId={} requestId={} repeated request, returning {}",
                earlier.getId(),
                earlier.getRequestId(),
                earlier.getStatus());
        return bookingMapper.toResponse(earlier);
    }

    private Booking getOwn(Long userId, Long bookingId) {
        return bookingRepository
                .findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
    }
}
