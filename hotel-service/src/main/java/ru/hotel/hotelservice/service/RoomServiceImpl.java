package ru.hotel.hotelservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.hotelservice.domain.Hotel;
import ru.hotel.hotelservice.domain.Room;
import ru.hotel.hotelservice.dto.RoomRequest;
import ru.hotel.hotelservice.dto.RoomResponse;
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
