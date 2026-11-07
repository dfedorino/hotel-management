package ru.hotel.hotelservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import ru.hotel.hotelservice.domain.Hotel;
import ru.hotel.hotelservice.domain.ReservationStatus;
import ru.hotel.hotelservice.domain.Room;
import ru.hotel.hotelservice.domain.RoomReservation;

@DataJpaTest
class FreeRoomQueryTest {

    private static final LocalDate START = LocalDate.of(2027, 1, 10);
    private static final LocalDate END = LocalDate.of(2027, 1, 15);
    private static final Sort BY_ID = Sort.by("id");

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomReservationRepository reservationRepository;

    private Hotel hotel;

    @BeforeEach
    void setUp() {
        hotel = hotelRepository.save(new Hotel("Grand", "Main st. 1"));
    }

    @Test
    void excludesRoomWithOverlappingActiveReservation() {
        // given
        Room busy = roomRepository.save(new Room(hotel, "101"));
        Room free = roomRepository.save(new Room(hotel, "102"));
        reserve(busy, START.plusDays(2), END.plusDays(2), ReservationStatus.ACTIVE);

        // when
        List<Room> rooms = roomRepository.findFree(null, START, END, BY_ID);

        // then
        assertThat(rooms).containsExactly(free);
    }

    @Test
    void includesRoomWhoseReservationIsReleasedOrAdjacent() {
        // given
        Room released = roomRepository.save(new Room(hotel, "101"));
        Room adjacent = roomRepository.save(new Room(hotel, "102"));
        reserve(released, START, END, ReservationStatus.RELEASED);
        reserve(adjacent, END, END.plusDays(3), ReservationStatus.ACTIVE);

        // when
        List<Room> rooms = roomRepository.findFree(null, START, END, BY_ID);

        // then
        assertThat(rooms).containsExactly(released, adjacent);
    }

    @Test
    void excludesRoomsOutOfService() {
        // given
        Room broken = new Room(hotel, "101");
        broken.setAvailable(false);
        roomRepository.save(broken);

        // when
        List<Room> rooms = roomRepository.findFree(null, START, END, BY_ID);

        // then
        assertThat(rooms).isEmpty();
    }

    @Test
    void filtersByHotelAndSortsByTimesBookedThenId() {
        // given
        Hotel other = hotelRepository.save(new Hotel("Plaza", "Second st. 2"));
        Room popular = roomRepository.save(roomBooked("101", 5));
        Room rareFirst = roomRepository.save(roomBooked("102", 1));
        Room rareSecond = roomRepository.save(roomBooked("103", 1));
        roomRepository.save(new Room(other, "201"));

        // when
        List<Room> rooms = roomRepository.findFree(
                hotel.getId(), START, END, Sort.by("timesBooked", "id"));

        // then
        assertThat(rooms).containsExactly(rareFirst, rareSecond, popular);
    }

    @Test
    void findAvailableIgnoresReservationsWhenNoDatesGiven() {
        // given
        Room reserved = roomRepository.save(new Room(hotel, "101"));
        reserve(reserved, START, END, ReservationStatus.ACTIVE);

        // when
        List<Room> rooms = roomRepository.findAvailable(null, BY_ID);

        // then
        assertThat(rooms).containsExactly(reserved);
    }

    @Test
    void findActiveOverlappingReturnsOnlyActiveReservationsOfHotelInPeriod() {
        // given
        Hotel other = hotelRepository.save(new Hotel("Plaza", "Second st. 2"));
        Room mine = roomRepository.save(new Room(hotel, "101"));
        Room foreign = roomRepository.save(new Room(other, "201"));
        reserve(mine, START.minusDays(2), START.plusDays(1), ReservationStatus.ACTIVE);
        reservationRepository.save(new RoomReservation(mine, 2L, "released", START, END));
        reservationRepository.findByRequestId("released").orElseThrow().setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(new RoomReservation(mine, 3L, "later", END, END.plusDays(1)));
        reserve(foreign, START, END, ReservationStatus.ACTIVE);

        // when
        List<RoomReservation> found = reservationRepository.findActiveOverlapping(hotel.getId(), START, END);

        // then
        assertThat(found).extracting(RoomReservation::getRequestId).containsExactly("req-101");
    }

    private Room roomBooked(String number, int timesBooked) {
        Room room = new Room(hotel, number);
        room.setTimesBooked(timesBooked);
        return room;
    }

    private void reserve(Room room, LocalDate start, LocalDate end, ReservationStatus status) {
        RoomReservation reservation = new RoomReservation(room, 1L, "req-" + room.getNumber(), start, end);
        reservation.setStatus(status);
        reservationRepository.save(reservation);
    }
}
