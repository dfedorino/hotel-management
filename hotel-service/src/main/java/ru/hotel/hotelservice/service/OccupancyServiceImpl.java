package ru.hotel.hotelservice.service;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.hotelservice.domain.Room;
import ru.hotel.hotelservice.domain.RoomReservation;
import ru.hotel.hotelservice.dto.OccupancyQuery;
import ru.hotel.hotelapi.model.OccupancyReport;
import ru.hotel.hotelapi.model.RoomOccupancy;
import ru.hotel.hotelservice.repository.RoomRepository;
import ru.hotel.hotelservice.repository.RoomReservationRepository;

@Service
@RequiredArgsConstructor
public class OccupancyServiceImpl implements OccupancyService {

    private static final double PRECISION = 10_000.0;

    private final RoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;

    @Override
    @Transactional(readOnly = true)
    public OccupancyReport report(OccupancyQuery query) {
        LocalDate start = query.startDate();
        LocalDate end = query.endDate();
        Periods.requireStartBeforeEnd(start, end);
        long periodNights = DAYS.between(start, end);
        List<Room> rooms = roomRepository.findAllInHotel(query.hotelId(), Sort.by("id"));
        Map<Long, Long> nightsByRoom = reservationRepository.findActiveOverlapping(query.hotelId(), start, end)
                .stream()
                .collect(groupingBy(r -> r.getRoom().getId(), summingLong(r -> nightsInside(r, start, end))));

        List<RoomOccupancy> occupancies = rooms.stream()
                .map(room -> occupancy(room, nightsByRoom.getOrDefault(room.getId(), 0L), periodNights))
                .toList();
        double average = occupancies.stream().mapToDouble(RoomOccupancy::getOccupancyRate).average().orElse(0.0);
        return new OccupancyReport(start, end, periodNights, rooms.size(), round(average), occupancies);
    }

    private static RoomOccupancy occupancy(Room room, long bookedNights, long periodNights) {
        return new RoomOccupancy(room.getId(), room.getHotel().getId(), room.getNumber(),
                room.getTimesBooked(), bookedNights, round((double) bookedNights / periodNights));
    }

    private static long nightsInside(RoomReservation reservation, LocalDate start, LocalDate end) {
        LocalDate from = reservation.getStartDate().isAfter(start) ? reservation.getStartDate() : start;
        LocalDate to = reservation.getEndDate().isBefore(end) ? reservation.getEndDate() : end;
        return DAYS.between(from, to);
    }

    private static double round(double value) {
        return Math.round(value * PRECISION) / PRECISION;
    }
}
