package ru.hotel.hotelservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.hotel.hotelservice.domain.Hotel;
import ru.hotel.hotelservice.domain.ReservationStatus;
import ru.hotel.hotelservice.domain.Room;
import ru.hotel.hotelservice.dto.AvailabilityRequest;
import ru.hotel.hotelservice.dto.ReleaseRequest;
import ru.hotel.hotelservice.dto.ReservationResponse;
import ru.hotel.hotelservice.exception.ConflictException;
import ru.hotel.hotelservice.exception.NotFoundException;
import ru.hotel.hotelservice.repository.HotelRepository;
import ru.hotel.hotelservice.repository.RoomRepository;
import ru.hotel.hotelservice.repository.RoomReservationRepository;

@SpringBootTest
class RoomAvailabilityServiceIntegrationTest {

    private static final LocalDate START = LocalDate.of(2027, 1, 10);
    private static final LocalDate END = LocalDate.of(2027, 1, 15);

    @Autowired
    private RoomAvailabilityService availabilityService;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomReservationRepository reservationRepository;

    private Room room;

    @BeforeEach
    void setUp() {
        Hotel hotel = hotelRepository.save(new Hotel("Grand", "Main st. 1"));
        room = roomRepository.save(new Room(hotel, "101"));
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        hotelRepository.deleteAll();
    }

    @Test
    void confirmLocksSlotAndCountsBooking() {
        // when
        ReservationResponse response = availabilityService.confirm(room.getId(), request("req-1", START, END));

        // then
        assertThat(response.status()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(response.roomId()).isEqualTo(room.getId());
        assertThat(timesBooked()).isEqualTo(1);
    }

    @Test
    void repeatedConfirmWithSameRequestIdIsIdempotent() {
        // given
        ReservationResponse first = availabilityService.confirm(room.getId(), request("req-1", START, END));

        // when
        ReservationResponse second = availabilityService.confirm(room.getId(), request("req-1", START, END));

        // then
        assertThat(second).isEqualTo(first);
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(timesBooked()).isEqualTo(1);
    }

    @Test
    void confirmRejectsReusedRequestIdWithDifferentDates() {
        // given
        availabilityService.confirm(room.getId(), request("req-1", START, END));

        // when / then
        assertThatThrownBy(() -> availabilityService.confirm(
                room.getId(), request("req-1", START.plusDays(10), END.plusDays(10))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void confirmRejectsOverlappingDates() {
        // given
        availabilityService.confirm(room.getId(), request("req-1", START, END));

        // when / then
        assertThatThrownBy(() -> availabilityService.confirm(
                room.getId(), request("req-2", START.plusDays(1), END.plusDays(1))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not available");
        assertThat(timesBooked()).isEqualTo(1);
    }

    @Test
    void confirmAllowsAdjacentDates() {
        // given
        availabilityService.confirm(room.getId(), request("req-1", START, END));

        // when
        availabilityService.confirm(room.getId(), request("req-2", END, END.plusDays(2)));

        // then
        assertThat(timesBooked()).isEqualTo(2);
    }

    @Test
    void confirmRejectsRoomOutOfService() {
        // given
        room.setAvailable(false);
        roomRepository.save(room);

        // when / then
        assertThatThrownBy(() -> availabilityService.confirm(room.getId(), request("req-1", START, END)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void confirmFailsForMissingRoom() {
        // when / then
        assertThatThrownBy(() -> availabilityService.confirm(-1L, request("req-1", START, END)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void releaseFreesSlotAndRevertsCounterOnlyOnce() {
        // given
        availabilityService.confirm(room.getId(), request("req-1", START, END));

        // when
        availabilityService.release(room.getId(), new ReleaseRequest("req-1"));
        availabilityService.release(room.getId(), new ReleaseRequest("req-1"));

        // then
        assertThat(reservationRepository.findByRequestId("req-1").orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.RELEASED);
        assertThat(timesBooked()).isZero();
        availabilityService.confirm(room.getId(), request("req-2", START, END));
    }

    @Test
    void releaseOfUnknownRequestIsNoOp() {
        // when
        availabilityService.release(room.getId(), new ReleaseRequest("never-confirmed"));

        // then
        assertThat(reservationRepository.count()).isZero();
    }

    @Test
    void concurrentOverlappingConfirmsLetExactlyOneThrough() throws Exception {
        // given
        int threads = 8;
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Callable<Boolean>> attempts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String requestId = "req-" + i;
            attempts.add(() -> {
                startSignal.await();
                try {
                    availabilityService.confirm(room.getId(), request(requestId, START, END));
                    return true;
                } catch (ConflictException ex) {
                    return false;
                }
            });
        }

        // when
        List<Boolean> results = runConcurrently(attempts, startSignal);

        // then
        assertThat(results).containsOnlyOnce(true);
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(timesBooked()).isEqualTo(1);
    }

    private static List<Boolean> runConcurrently(List<Callable<Boolean>> tasks, CountDownLatch startSignal)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Callable<Boolean> task : tasks) {
                futures.add(executor.submit(task));
            }
            startSignal.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private int timesBooked() {
        return roomRepository.findById(room.getId()).orElseThrow().getTimesBooked();
    }

    private static AvailabilityRequest request(String requestId, LocalDate start, LocalDate end) {
        return new AvailabilityRequest(requestId, 100L, start, end);
    }
}
