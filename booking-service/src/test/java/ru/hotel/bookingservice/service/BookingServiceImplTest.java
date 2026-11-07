package ru.hotel.bookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import ru.hotel.bookingapi.model.BookingPage;
import ru.hotel.bookingapi.model.BookingRequest;
import ru.hotel.bookingapi.model.BookingResponse;
import ru.hotel.bookingservice.domain.Booking;
import ru.hotel.bookingservice.exception.BadRequestException;
import ru.hotel.bookingservice.exception.ConflictException;
import ru.hotel.bookingservice.exception.NotFoundException;
import ru.hotel.bookingservice.exception.ServiceUnavailableException;
import ru.hotel.bookingservice.mapper.BookingMapper;
import ru.hotel.bookingservice.repository.BookingRepository;
import ru.hotel.hotelapi.error.HotelServiceConflictException;
import ru.hotel.hotelapi.model.ErrorResponse;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final Instant NOW = Instant.parse("2027-01-01T10:00:00Z");
    private static final LocalDate START = LocalDate.of(2027, 1, 10);
    private static final LocalDate END = LocalDate.of(2027, 1, 15);
    private static final Long USER_ID = 1L;

    @Mock
    private BookingStore store;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HotelAvailability hotel;

    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                store,
                bookingRepository,
                hotel,
                Mappers.getMapper(BookingMapper.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createRecordsPendingThenConfirmsAfterHotelHoldsTheSlot() {
        // given
        Booking pending = booking(10L, 5L, "req-1");
        given(store.findByRequestId("req-1")).willReturn(Optional.empty());
        given(store.createPending(USER_ID, 5L, START, END, "req-1")).willReturn(pending);
        given(store.markConfirmed(10L)).willAnswer(invocation -> confirmed(pending));

        // when
        BookingResponse response = bookingService.create(USER_ID, request(5L).requestId("req-1"));

        // then
        assertThat(response.getStatus()).isEqualTo(ru.hotel.bookingapi.model.BookingStatus.CONFIRMED);
        InOrder saga = inOrder(store, hotel);
        saga.verify(store).createPending(USER_ID, 5L, START, END, "req-1");
        saga.verify(hotel).confirm(5L, 10L, "req-1", START, END);
        saga.verify(store).markConfirmed(10L);
    }

    @Test
    void createCancelsWithoutCompensationWhenHotelRejectsTheDates() {
        // given
        given(store.findByRequestId("req-1")).willReturn(Optional.empty());
        given(store.createPending(USER_ID, 5L, START, END, "req-1")).willReturn(booking(10L, 5L, "req-1"));
        willThrow(new HotelServiceConflictException(hotelError(409, "Room 5 is not available")))
                .given(hotel)
                .confirm(5L, 10L, "req-1", START, END);
        BookingRequest request = request(5L).requestId("req-1");

        // when / then
        assertThatThrownBy(() -> bookingService.create(USER_ID, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Room 5 is not available");
        verify(store).markCancelled(10L);
        verify(hotel, never()).release(anyLong(), anyString());
        verify(store, never()).markConfirmed(anyLong());
    }

    @Test
    void createCompensatesWhenHotelServiceDoesNotAnswer() {
        // given
        given(store.findByRequestId("req-1")).willReturn(Optional.empty());
        given(store.createPending(USER_ID, 5L, START, END, "req-1")).willReturn(booking(10L, 5L, "req-1"));
        willThrow(new IllegalStateException("read timed out")).given(hotel).confirm(5L, 10L, "req-1", START, END);
        BookingRequest request = request(5L).requestId("req-1");

        // when / then
        assertThatThrownBy(() -> bookingService.create(USER_ID, request))
                .isInstanceOf(ServiceUnavailableException.class);
        InOrder compensation = inOrder(hotel, store);
        compensation.verify(hotel).release(5L, "req-1");
        compensation.verify(store).markCancelled(10L);
    }

    @Test
    void bookingIsCancelledEvenIfCompensationFails() {
        // given
        given(store.findByRequestId("req-1")).willReturn(Optional.empty());
        given(store.createPending(USER_ID, 5L, START, END, "req-1")).willReturn(booking(10L, 5L, "req-1"));
        willThrow(new IllegalStateException("down")).given(hotel).confirm(5L, 10L, "req-1", START, END);
        willThrow(new IllegalStateException("still down")).given(hotel).release(5L, "req-1");
        BookingRequest request = request(5L).requestId("req-1");

        // when / then
        assertThatThrownBy(() -> bookingService.create(USER_ID, request))
                .isInstanceOf(ServiceUnavailableException.class);
        verify(store).markCancelled(10L);
    }

    @Test
    void repeatedRequestReturnsTheOriginalBookingWithoutCallingHotelService() {
        // given
        given(store.findByRequestId("req-1")).willReturn(Optional.of(confirmed(booking(10L, 5L, "req-1"))));

        // when
        BookingResponse response = bookingService.create(USER_ID, request(5L).requestId("req-1"));

        // then
        assertThat(response.getId()).isEqualTo(10L);
        verifyNoInteractions(hotel);
        verify(store, never()).createPending(any(), any(), any(), any(), any());
    }

    @Test
    void concurrentDuplicateLosesTheInsertAndReturnsTheWinner() {
        // given
        Booking winner = booking(10L, 5L, "req-1");
        given(store.findByRequestId("req-1")).willReturn(Optional.empty(), Optional.of(winner));
        given(store.createPending(USER_ID, 5L, START, END, "req-1"))
                .willThrow(new DataIntegrityViolationException("duplicate request_id"));

        // when
        BookingResponse response = bookingService.create(USER_ID, request(5L).requestId("req-1"));

        // then
        assertThat(response.getId()).isEqualTo(10L);
        verifyNoInteractions(hotel);
    }

    @Test
    void requestIdOfAnotherUserIsRejected() {
        // given
        Booking foreign = new Booking(2L, 5L, START, END, "req-1", NOW);
        given(store.findByRequestId("req-1")).willReturn(Optional.of(foreign));
        BookingRequest request = request(5L).requestId("req-1");

        // when / then
        assertThatThrownBy(() -> bookingService.create(USER_ID, request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void missingRequestIdIsGenerated() {
        // given
        given(store.findByRequestId(anyString())).willReturn(Optional.empty());
        given(store.createPending(eq(USER_ID), eq(5L), eq(START), eq(END), anyString()))
                .willAnswer(invocation -> booking(10L, 5L, invocation.getArgument(4)));
        given(store.markConfirmed(10L)).willAnswer(invocation -> confirmed(booking(10L, 5L, "generated")));

        // when
        bookingService.create(USER_ID, request(5L));

        // then
        verify(hotel).confirm(eq(5L), eq(10L), matches("[0-9a-f-]{36}"), eq(START), eq(END));
    }

    @Test
    void autoSelectBooksTheLeastBookedFreeRoomAndIgnoresRoomId() {
        // given
        given(store.findByRequestId("req-1")).willReturn(Optional.empty());
        given(hotel.recommendRooms(1L, START, END)).willReturn(List.of(8L, 3L));
        given(store.createPending(USER_ID, 8L, START, END, "req-1")).willReturn(booking(10L, 8L, "req-1"));
        given(store.markConfirmed(10L)).willAnswer(invocation -> confirmed(booking(10L, 8L, "req-1")));
        BookingRequest request = request(99L).autoSelect(true).hotelId(1L).requestId("req-1");

        // when
        BookingResponse response = bookingService.create(USER_ID, request);

        // then
        assertThat(response.getRoomId()).isEqualTo(8L);
        verify(hotel).confirm(8L, 10L, "req-1", START, END);
    }

    @Test
    void autoSelectWithoutFreeRoomsIsAConflict() {
        // given
        given(store.findByRequestId("req-1")).willReturn(Optional.empty());
        given(hotel.recommendRooms(null, START, END)).willReturn(List.of());
        BookingRequest request = new BookingRequest(START, END).autoSelect(true).requestId("req-1");

        // when / then
        assertThatThrownBy(() -> bookingService.create(USER_ID, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("No free rooms for the requested dates");
    }

    @Test
    void roomIdIsRequiredWithoutAutoSelect() {
        // given
        BookingRequest request = new BookingRequest(START, END);

        // when / then
        assertThatThrownBy(() -> bookingService.create(USER_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("roomId is required unless autoSelect is true");
    }

    @Test
    void datesAreValidatedBeforeAnythingIsStored() {
        // given
        BookingRequest reversed = new BookingRequest(END, START).roomId(5L);
        BookingRequest past = new BookingRequest(LocalDate.of(2026, 12, 31), END).roomId(5L);

        // when / then
        assertThatThrownBy(() -> bookingService.create(USER_ID, reversed))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("startDate must be before endDate");
        assertThatThrownBy(() -> bookingService.create(USER_ID, past))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("startDate must not be in the past");
        verifyNoInteractions(store, hotel);
    }

    @Test
    void cancelReleasesTheSlotThenCancels() {
        // given
        Booking booking = confirmed(booking(10L, 5L, "req-1"));
        given(bookingRepository.findByIdAndUserId(10L, USER_ID)).willReturn(Optional.of(booking));

        // when
        bookingService.cancel(USER_ID, 10L);

        // then
        InOrder cancel = inOrder(hotel, store);
        cancel.verify(hotel).release(5L, "req-1");
        cancel.verify(store).markCancelled(10L);
    }

    @Test
    void cancellingCancelledBookingDoesNothing() {
        // given
        Booking booking = booking(10L, 5L, "req-1");
        booking.cancel();
        given(bookingRepository.findByIdAndUserId(10L, USER_ID)).willReturn(Optional.of(booking));

        // when
        bookingService.cancel(USER_ID, 10L);

        // then
        verifyNoInteractions(hotel);
        verify(store, never()).markCancelled(anyLong());
    }

    @Test
    void cancelKeepsBookingWhenSlotCannotBeReleased() {
        // given
        given(bookingRepository.findByIdAndUserId(10L, USER_ID))
                .willReturn(Optional.of(confirmed(booking(10L, 5L, "req-1"))));
        willThrow(new IllegalStateException("down")).given(hotel).release(5L, "req-1");

        // when / then
        assertThatThrownBy(() -> bookingService.cancel(USER_ID, 10L)).isInstanceOf(ServiceUnavailableException.class);
        verify(store, never()).markCancelled(anyLong());
    }

    @Test
    void bookingsOfOtherUsersAreNotFound() {
        // given
        given(bookingRepository.findByIdAndUserId(10L, USER_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> bookingService.findById(USER_ID, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Booking 10 not found");
        assertThatThrownBy(() -> bookingService.cancel(USER_ID, 10L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void historyIsPagedNewestFirst() {
        // given
        PageRequest pageRequest = PageRequest.of(1, 2, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Booking> page = new PageImpl<>(List.of(booking(10L, 5L, "req-1")), pageRequest, 3);
        given(bookingRepository.findByUserId(USER_ID, pageRequest)).willReturn(page);

        // when
        BookingPage result = bookingService.findAll(USER_ID, 1, 2);

        // then
        assertThat(result.getContent()).extracting(BookingResponse::getId).containsExactly(10L);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(3L);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    private static BookingRequest request(Long roomId) {
        return new BookingRequest(START, END).roomId(roomId);
    }

    private static Booking booking(Long id, Long roomId, String requestId) {
        Booking booking = new Booking(USER_ID, roomId, START, END, requestId, NOW);
        booking.setId(id);
        return booking;
    }

    private static Booking confirmed(Booking booking) {
        booking.confirm();
        return booking;
    }

    private static ErrorResponse hotelError(int status, String message) {
        return new ErrorResponse(
                NOW.atOffset(ZoneOffset.UTC), status, "Conflict", message, "/api/rooms/5/confirm-availability");
    }
}
