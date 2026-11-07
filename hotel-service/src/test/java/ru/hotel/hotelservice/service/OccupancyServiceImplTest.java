package ru.hotel.hotelservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.hotel.hotelapi.model.OccupancyReport;
import ru.hotel.hotelapi.model.RoomOccupancy;
import ru.hotel.hotelservice.domain.Hotel;
import ru.hotel.hotelservice.domain.Room;
import ru.hotel.hotelservice.domain.RoomReservation;
import ru.hotel.hotelservice.dto.OccupancyQuery;
import ru.hotel.hotelservice.exception.BadRequestException;
import ru.hotel.hotelservice.repository.RoomRepository;
import ru.hotel.hotelservice.repository.RoomReservationRepository;

@ExtendWith(MockitoExtension.class)
class OccupancyServiceImplTest {

    private static final LocalDate START = LocalDate.of(2027, 1, 1);
    private static final LocalDate END = LocalDate.of(2027, 1, 11);

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomReservationRepository reservationRepository;

    private OccupancyServiceImpl occupancyService;

    private Room busy;
    private Room idle;

    @BeforeEach
    void setUp() {
        occupancyService = new OccupancyServiceImpl(roomRepository, reservationRepository);
        Hotel hotel = new Hotel("Grand", "Main st. 1");
        hotel.setId(1L);
        busy = room(hotel, 10L, "101", 2);
        idle = room(hotel, 11L, "102", 0);
    }

    @Test
    void countsOnlyNightsInsideThePeriod() {
        // given: one stay fully inside (3 nights), one crossing the period start (2 nights inside)
        given(roomRepository.findAllInHotel(1L, Sort.by("id"))).willReturn(List.of(busy, idle));
        given(reservationRepository.findActiveOverlapping(1L, START, END))
                .willReturn(List.of(
                        reservation(busy, LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 8)),
                        reservation(busy, LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 3))));

        // when
        OccupancyReport report = occupancyService.report(new OccupancyQuery(1L, START, END));

        // then
        assertThat(report.getPeriodNights()).isEqualTo(10L);
        assertThat(report.getTotalRooms()).isEqualTo(2);
        assertThat(report.getRooms())
                .containsExactly(
                        new RoomOccupancy(10L, 1L, "101", 2, 5L, 0.5), new RoomOccupancy(11L, 1L, "102", 0, 0L, 0.0));
        assertThat(report.getAverageOccupancy()).isEqualTo(0.25);
    }

    @Test
    void reportsZeroAverageWhenThereAreNoRooms() {
        // given
        given(roomRepository.findAllInHotel(null, Sort.by("id"))).willReturn(List.of());
        given(reservationRepository.findActiveOverlapping(null, START, END)).willReturn(List.of());

        // when
        OccupancyReport report = occupancyService.report(new OccupancyQuery(null, START, END));

        // then
        assertThat(report.getTotalRooms()).isZero();
        assertThat(report.getAverageOccupancy()).isZero();
    }

    @Test
    void rejectsPeriodWhereStartIsNotBeforeEnd() {
        // given
        OccupancyQuery reversed = new OccupancyQuery(null, END, START);

        // when / then
        assertThatThrownBy(() -> occupancyService.report(reversed))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("startDate must be before endDate");
    }

    private static Room room(Hotel hotel, Long id, String number, int timesBooked) {
        Room room = new Room(hotel, number);
        room.setId(id);
        room.setTimesBooked(timesBooked);
        return room;
    }

    private static RoomReservation reservation(Room room, LocalDate start, LocalDate end) {
        return new RoomReservation(room, 1L, "req-" + start, start, end);
    }
}
