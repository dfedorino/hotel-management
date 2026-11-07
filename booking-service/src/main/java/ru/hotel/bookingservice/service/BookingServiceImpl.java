package ru.hotel.bookingservice.service;

import java.time.Clock;
import java.time.LocalDate;
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
import ru.hotel.bookingservice.exception.ApiException;
import ru.hotel.bookingservice.exception.BadRequestException;
import ru.hotel.bookingservice.exception.ConflictException;
import ru.hotel.bookingservice.exception.NotFoundException;
import ru.hotel.bookingservice.exception.ServiceUnavailableException;
import ru.hotel.bookingservice.mapper.BookingMapper;
import ru.hotel.bookingservice.repository.BookingRepository;
import ru.hotel.hotelapi.error.HotelServiceClientException;

/**
 * Booking saga orchestrated by this service as a sequence of local transactions (see {@link BookingStore}):
 *
 * <ol>
 *   <li>record the booking as PENDING;
 *   <li>ask Hotel Service to hold the slot;
 *   <li>CONFIRMED on success; CANCELLED if Hotel Service rejects the request; if it does not answer, release the slot
 *       (it may have been held before the answer was lost) and CANCELLED.
 * </ol>
 *
 * All steps of one booking are logged with its {@code bookingId} in the MDC.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    static final String BOOKING_ID = "bookingId";
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final BookingStore store;
    private final BookingRepository bookingRepository;
    private final HotelAvailability hotel;
    private final BookingMapper bookingMapper;
    private final Clock clock;

    @Override
    public BookingResponse create(Long userId, BookingRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        String requestId = Optional.ofNullable(request.getRequestId())
                .orElseGet(() -> UUID.randomUUID().toString());
        Optional<Booking> earlier = store.findByRequestId(requestId);
        if (earlier.isPresent()) {
            return replay(earlier.get(), userId);
        }
        Long roomId = chooseRoom(request);
        Booking booking;
        try {
            booking = store.createPending(userId, roomId, request.getStartDate(), request.getEndDate(), requestId);
        } catch (DataIntegrityViolationException duplicate) {
            // a concurrent delivery of the same request won the insert
            return replay(store.findByRequestId(requestId).orElseThrow(() -> duplicate), userId);
        }
        try (MDC.MDCCloseable ignored = MDC.putCloseable(BOOKING_ID, String.valueOf(booking.getId()))) {
            log.info("requestId={} roomId={} booking PENDING", requestId, roomId);
            return bookingMapper.toResponse(holdSlot(booking));
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
                hotel.release(booking.getRoomId(), booking.getRequestId());
            } catch (RuntimeException ex) {
                // the booking stays as it was, so the user can simply retry the cancellation
                throw unavailable(ex);
            }
            store.markCancelled(bookingId);
            log.info("requestId={} booking CANCELLED by user", booking.getRequestId());
        }
    }

    private Booking holdSlot(Booking booking) {
        try {
            hotel.confirm(
                    booking.getRoomId(),
                    booking.getId(),
                    booking.getRequestId(),
                    booking.getStartDate(),
                    booking.getEndDate());
        } catch (HotelServiceClientException rejected) {
            store.markCancelled(booking.getId());
            log.info("requestId={} booking CANCELLED: {}", booking.getRequestId(), rejected.getMessage());
            throw toApiException(rejected);
        } catch (RuntimeException failure) {
            compensate(booking, failure);
            throw new ServiceUnavailableException("Hotel Service is unavailable, the booking was cancelled", failure);
        }
        Booking confirmed = store.markConfirmed(booking.getId());
        log.info("requestId={} booking CONFIRMED", booking.getRequestId());
        return confirmed;
    }

    /** The slot may have been held even though the answer was lost, so it is released before cancelling. */
    private void compensate(Booking booking, RuntimeException failure) {
        log.warn("requestId={} Hotel Service failed: {}, compensating", booking.getRequestId(), failure.toString());
        try {
            hotel.release(booking.getRoomId(), booking.getRequestId());
            log.info("requestId={} compensation done: slot released", booking.getRequestId());
        } catch (RuntimeException releaseFailure) {
            log.error(
                    "requestId={} compensation failed, slot may stay held in Hotel Service: {}",
                    booking.getRequestId(),
                    releaseFailure.toString());
        } finally {
            store.markCancelled(booking.getId());
            log.info("requestId={} booking CANCELLED", booking.getRequestId());
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

    private Long chooseRoom(BookingRequest request) {
        if (Boolean.TRUE.equals(request.getAutoSelect())) {
            return pickLeastBookedRoom(request);
        }
        if (request.getRoomId() == null) {
            throw new BadRequestException("roomId is required unless autoSelect is true");
        }
        return request.getRoomId();
    }

    private Long pickLeastBookedRoom(BookingRequest request) {
        List<Long> candidates;
        try {
            candidates = hotel.recommendRooms(request.getHotelId(), request.getStartDate(), request.getEndDate());
        } catch (HotelServiceClientException rejected) {
            throw toApiException(rejected);
        } catch (RuntimeException failure) {
            throw unavailable(failure);
        }
        return candidates.stream()
                .findFirst()
                .orElseThrow(() -> new ConflictException("No free rooms for the requested dates"));
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (!start.isBefore(end)) {
            throw new BadRequestException("startDate must be before endDate");
        }
        if (start.isBefore(LocalDate.now(clock))) {
            throw new BadRequestException("startDate must not be in the past");
        }
    }

    private Booking getOwn(Long userId, Long bookingId) {
        return bookingRepository
                .findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
    }

    private static ApiException toApiException(HotelServiceClientException rejected) {
        return switch (rejected.getStatus()) {
            case 404 -> new NotFoundException(rejected.getMessage(), rejected);
            case 409 -> new ConflictException(rejected.getMessage(), rejected);
            default -> new BadRequestException(rejected.getMessage(), rejected);
        };
    }

    private static ServiceUnavailableException unavailable(RuntimeException cause) {
        log.warn("Hotel Service is unavailable: {}", cause.toString());
        return new ServiceUnavailableException("Hotel Service is unavailable, try again later", cause);
    }
}
