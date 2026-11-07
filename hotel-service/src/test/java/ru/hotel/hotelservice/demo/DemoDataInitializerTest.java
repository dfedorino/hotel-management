package ru.hotel.hotelservice.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import ru.hotel.hotelservice.repository.HotelRepository;
import ru.hotel.hotelservice.repository.RoomRepository;
import ru.hotel.hotelservice.repository.RoomReservationRepository;

@DataJpaTest
class DemoDataInitializerTest {

    private static final LocalDate TODAY = LocalDate.of(2027, 3, 1);

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomReservationRepository reservationRepository;

    private DemoDataInitializer initializer;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        initializer = new DemoDataInitializer(hotelRepository, roomRepository, reservationRepository, clock);
    }

    @Test
    void seedsHotelsRoomsAndUpcomingReservations() {
        // when
        initializer.run(null);

        // then
        assertThat(hotelRepository.findAll()).extracting(Hotel::getName)
                .containsExactlyInAnyOrder("Grand Plaza", "Seaside Resort");
        List<Room> rooms = roomRepository.findAll();
        assertThat(rooms).hasSize(8);
        assertThat(rooms).filteredOn(room -> !room.isAvailable()).extracting(Room::getNumber).containsExactly("105");
        List<RoomReservation> reservations = reservationRepository.findAll();
        assertThat(reservations).isNotEmpty()
                .allMatch(r -> r.getStatus() == ReservationStatus.ACTIVE)
                .allMatch(r -> r.getStartDate().isAfter(TODAY));
    }

    @Test
    void recommendationOrderIsVisibleInSeedData() {
        // when
        initializer.run(null);

        // then: rooms differ in times booked, so recommendations are not just ordered by id
        List<Room> recommended = roomRepository.findAvailable(null, Sort.by("timesBooked", "id"));
        List<Room> byId = roomRepository.findAvailable(null, Sort.by("id"));
        assertThat(recommended).isNotEqualTo(byId);
    }

    @Test
    void doesNothingWhenDataAlreadyExists() {
        // given
        hotelRepository.save(new Hotel("Existing", "Somewhere"));

        // when
        initializer.run(null);
        initializer.run(null);

        // then
        assertThat(hotelRepository.count()).isEqualTo(1);
        assertThat(roomRepository.count()).isZero();
    }
}
