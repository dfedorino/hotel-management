package ru.hotel.hotelservice.service;

import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.hotelservice.domain.ReservationStatus;
import ru.hotel.hotelservice.domain.Room;
import ru.hotel.hotelservice.domain.RoomReservation;
import ru.hotel.hotelservice.dto.AvailabilityRequest;
import ru.hotel.hotelservice.dto.ReleaseRequest;
import ru.hotel.hotelservice.dto.ReservationResponse;
import ru.hotel.hotelservice.exception.ConflictException;
import ru.hotel.hotelservice.exception.NotFoundException;
import ru.hotel.hotelservice.mapper.ReservationMapper;
import ru.hotel.hotelservice.repository.RoomRepository;
import ru.hotel.hotelservice.repository.RoomReservationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomAvailabilityServiceImpl implements RoomAvailabilityService {

    private final RoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;

    @Override
    @Transactional
    public ReservationResponse confirm(Long roomId, AvailabilityRequest request) {
        Room room = lockRoom(roomId);
        Optional<RoomReservation> existing = reservationRepository.findByRequestId(request.requestId());
        if (existing.isPresent()) {
            return replay(existing.get(), roomId, request);
        }
        if (!room.isAvailable()) {
            throw new ConflictException("Room " + roomId + " is out of service");
        }
        if (reservationRepository.existsActiveOverlapping(roomId, request.startDate(), request.endDate())) {
            log.info("bookingId={} requestId={} roomId={} slot rejected: dates overlap",
                    request.bookingId(), request.requestId(), roomId);
            throw new ConflictException("Room " + roomId + " is not available from "
                    + request.startDate() + " to " + request.endDate());
        }
        RoomReservation reservation = reservationRepository.save(new RoomReservation(
                room, request.bookingId(), request.requestId(), request.startDate(), request.endDate()));
        room.setTimesBooked(room.getTimesBooked() + 1);
        log.info("bookingId={} requestId={} roomId={} slot locked {}..{}",
                request.bookingId(), request.requestId(), roomId, request.startDate(), request.endDate());
        return reservationMapper.toResponse(reservation);
    }

    @Override
    @Transactional
    public void release(Long roomId, ReleaseRequest request) {
        Room room = lockRoom(roomId);
        Optional<RoomReservation> found = reservationRepository.findByRequestId(request.requestId());
        if (found.isEmpty()) {
            log.info("requestId={} roomId={} release skipped: nothing was locked", request.requestId(), roomId);
            return;
        }
        RoomReservation reservation = found.get();
        if (!Objects.equals(reservation.getRoom().getId(), roomId)) {
            throw new ConflictException("Request " + request.requestId() + " does not belong to room " + roomId);
        }
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            log.info("bookingId={} requestId={} roomId={} release skipped: already released",
                    reservation.getBookingId(), request.requestId(), roomId);
            return;
        }
        reservation.setStatus(ReservationStatus.RELEASED);
        room.setTimesBooked(Math.max(0, room.getTimesBooked() - 1));
        log.info("bookingId={} requestId={} roomId={} slot released",
                reservation.getBookingId(), request.requestId(), roomId);
    }

    private ReservationResponse replay(RoomReservation reservation, Long roomId, AvailabilityRequest request) {
        boolean sameRequest = Objects.equals(reservation.getRoom().getId(), roomId)
                && Objects.equals(reservation.getBookingId(), request.bookingId())
                && reservation.getStartDate().equals(request.startDate())
                && reservation.getEndDate().equals(request.endDate());
        if (!sameRequest) {
            throw new ConflictException("Request " + request.requestId() + " was already used with other parameters");
        }
        log.info("bookingId={} requestId={} roomId={} duplicate confirm, returning stored result",
                request.bookingId(), request.requestId(), roomId);
        return reservationMapper.toResponse(reservation);
    }

    private Room lockRoom(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new NotFoundException("Room " + roomId + " not found"));
    }
}
