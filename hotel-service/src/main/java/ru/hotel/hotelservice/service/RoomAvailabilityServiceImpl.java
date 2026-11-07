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
import ru.hotel.hotelapi.model.AvailabilityRequest;
import ru.hotel.hotelapi.model.ReleaseRequest;
import ru.hotel.hotelapi.model.ReservationResponse;
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
        Periods.requireStartBeforeEnd(request.getStartDate(), request.getEndDate());
        Room room = lockRoom(roomId);
        Optional<RoomReservation> existing = reservationRepository.findByRequestId(request.getRequestId());
        if (existing.isPresent()) {
            return replay(existing.get(), roomId, request);
        }
        if (!room.isAvailable()) {
            throw new ConflictException("Room " + roomId + " is out of service");
        }
        if (reservationRepository.existsActiveOverlapping(roomId, request.getStartDate(), request.getEndDate())) {
            log.info("bookingId={} requestId={} roomId={} slot rejected: dates overlap",
                    request.getBookingId(), request.getRequestId(), roomId);
            throw new ConflictException("Room " + roomId + " is not available from "
                    + request.getStartDate() + " to " + request.getEndDate());
        }
        RoomReservation reservation = reservationRepository.save(new RoomReservation(
                room, request.getBookingId(), request.getRequestId(), request.getStartDate(), request.getEndDate()));
        room.setTimesBooked(room.getTimesBooked() + 1);
        log.info("bookingId={} requestId={} roomId={} slot locked {}..{}",
                request.getBookingId(), request.getRequestId(), roomId, request.getStartDate(), request.getEndDate());
        return reservationMapper.toResponse(reservation);
    }

    @Override
    @Transactional
    public void release(Long roomId, ReleaseRequest request) {
        Room room = lockRoom(roomId);
        Optional<RoomReservation> found = reservationRepository.findByRequestId(request.getRequestId());
        if (found.isEmpty()) {
            log.info("requestId={} roomId={} release skipped: nothing was locked", request.getRequestId(), roomId);
            return;
        }
        RoomReservation reservation = found.get();
        if (!Objects.equals(reservation.getRoom().getId(), roomId)) {
            throw new ConflictException("Request " + request.getRequestId() + " does not belong to room " + roomId);
        }
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            log.info("bookingId={} requestId={} roomId={} release skipped: already released",
                    reservation.getBookingId(), request.getRequestId(), roomId);
            return;
        }
        reservation.setStatus(ReservationStatus.RELEASED);
        room.setTimesBooked(Math.max(0, room.getTimesBooked() - 1));
        log.info("bookingId={} requestId={} roomId={} slot released",
                reservation.getBookingId(), request.getRequestId(), roomId);
    }

    private ReservationResponse replay(RoomReservation reservation, Long roomId, AvailabilityRequest request) {
        boolean sameRequest = Objects.equals(reservation.getRoom().getId(), roomId)
                && Objects.equals(reservation.getBookingId(), request.getBookingId())
                && reservation.getStartDate().equals(request.getStartDate())
                && reservation.getEndDate().equals(request.getEndDate());
        if (!sameRequest) {
            throw new ConflictException("Request " + request.getRequestId() + " was already used with other parameters");
        }
        log.info("bookingId={} requestId={} roomId={} duplicate confirm, returning stored result",
                request.getBookingId(), request.getRequestId(), roomId);
        return reservationMapper.toResponse(reservation);
    }

    private Room lockRoom(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new NotFoundException("Room " + roomId + " not found"));
    }
}
