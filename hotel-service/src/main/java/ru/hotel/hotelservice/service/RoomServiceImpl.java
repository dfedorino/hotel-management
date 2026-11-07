package ru.hotel.hotelservice.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.hotelservice.domain.Hotel;
import ru.hotel.hotelservice.domain.Room;
import ru.hotel.hotelservice.dto.RoomRequest;
import ru.hotel.hotelservice.dto.RoomResponse;
import ru.hotel.hotelservice.dto.RoomSearch;
import ru.hotel.hotelservice.exception.BadRequestException;
import ru.hotel.hotelservice.exception.ConflictException;
import ru.hotel.hotelservice.exception.NotFoundException;
import ru.hotel.hotelservice.mapper.RoomMapper;
import ru.hotel.hotelservice.repository.HotelRepository;
import ru.hotel.hotelservice.repository.RoomRepository;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomMapper roomMapper;

    private static final Sort BY_ID = Sort.by("id");
    private static final Sort LEAST_BOOKED_FIRST = Sort.by("timesBooked", "id");

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> findFree(RoomSearch search) {
        return search(search, BY_ID);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> findRecommended(RoomSearch search) {
        return search(search, LEAST_BOOKED_FIRST);
    }

    private List<RoomResponse> search(RoomSearch search, Sort sort) {
        validateDates(search);
        List<Room> rooms = search.hasDates()
                ? roomRepository.findFree(search.hotelId(), search.startDate(), search.endDate(), sort)
                : roomRepository.findAvailable(search.hotelId(), sort);
        return rooms.stream().map(roomMapper::toResponse).toList();
    }

    private static void validateDates(RoomSearch search) {
        if ((search.startDate() == null) != (search.endDate() == null)) {
            throw new BadRequestException("startDate and endDate must be given together");
        }
        if (search.hasDates() && !search.startDate().isBefore(search.endDate())) {
            throw new BadRequestException("startDate must be before endDate");
        }
    }

    @Override
    @Transactional
    public RoomResponse create(RoomRequest request) {
        Hotel hotel = getHotel(request.hotelId());
        if (roomRepository.existsByHotelIdAndNumber(hotel.getId(), request.number())) {
            throw duplicate(request);
        }
        Room room = new Room(hotel, request.number());
        applyAvailability(room, request);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse findById(Long id) {
        return roomMapper.toResponse(getRoom(id));
    }

    @Override
    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = getRoom(id);
        Hotel hotel = getHotel(request.hotelId());
        if (roomRepository.existsByHotelIdAndNumberAndIdNot(hotel.getId(), request.number(), id)) {
            throw duplicate(request);
        }
        room.setHotel(hotel);
        room.setNumber(request.number());
        applyAvailability(room, request);
        return roomMapper.toResponse(room);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new NotFoundException("Room " + id + " not found");
        }
        roomRepository.deleteById(id);
    }

    private static void applyAvailability(Room room, RoomRequest request) {
        if (request.available() != null) {
            room.setAvailable(request.available());
        }
    }

    private Room getRoom(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new NotFoundException("Room " + id + " not found"));
    }

    private Hotel getHotel(Long id) {
        return hotelRepository.findById(id).orElseThrow(() -> new NotFoundException("Hotel " + id + " not found"));
    }

    private static ConflictException duplicate(RoomRequest request) {
        return new ConflictException(
                "Room " + request.number() + " already exists in hotel " + request.hotelId());
    }
}
