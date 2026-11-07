package ru.hotel.hotelservice.demo;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.hotelservice.domain.Hotel;
import ru.hotel.hotelservice.domain.Room;
import ru.hotel.hotelservice.domain.RoomReservation;
import ru.hotel.hotelservice.repository.HotelRepository;
import ru.hotel.hotelservice.repository.RoomRepository;
import ru.hotel.hotelservice.repository.RoomReservationRepository;

/**
 * Loads demo hotels on an empty database: uneven times_booked shows the recommendation order,
 * room 105 is out of service, and upcoming reservations (matching Booking Service demo bookings
 * 1..3 by bookingId and requestId) hide rooms from free search and fill the occupancy report.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hotel.demo-data.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (hotelRepository.count() > 0) {
            log.info("Demo data skipped: database is not empty");
            return;
        }
        LocalDate today = LocalDate.now(clock);

        Hotel plaza = hotelRepository.save(new Hotel("Grand Plaza", "Moscow, Tverskaya st. 1"));
        Room room101 = room(plaza, "101", 4, true);
        Room room102 = room(plaza, "102", 2, true);
        room(plaza, "103", 0, true);
        room(plaza, "104", 0, true);
        room(plaza, "105", 1, false);

        Hotel seaside = hotelRepository.save(new Hotel("Seaside Resort", "Sochi, Kurortny ave. 10"));
        Room room201 = room(seaside, "201", 3, true);
        room(seaside, "202", 1, true);
        room(seaside, "203", 0, true);

        reserve(room101, 1L, today.plusDays(7), today.plusDays(10));
        reserve(room201, 2L, today.plusDays(3), today.plusDays(5));
        reserve(room102, 3L, today.plusDays(14), today.plusDays(21));
        log.info("Demo data loaded: {} hotels, {} rooms, {} reservations",
                hotelRepository.count(), roomRepository.count(), reservationRepository.count());
    }

    private Room room(Hotel hotel, String number, int timesBooked, boolean available) {
        Room room = new Room(hotel, number);
        room.setTimesBooked(timesBooked);
        room.setAvailable(available);
        return roomRepository.save(room);
    }

    private void reserve(Room room, Long bookingId, LocalDate start, LocalDate end) {
        reservationRepository.save(new RoomReservation(room, bookingId, "demo-booking-" + bookingId, start, end));
    }
}
