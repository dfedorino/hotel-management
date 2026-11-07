package ru.hotel.hotelservice.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.hotelservice.domain.Hotel;
import ru.hotel.hotelservice.dto.HotelRequest;
import ru.hotel.hotelservice.dto.HotelResponse;
import ru.hotel.hotelservice.exception.ConflictException;
import ru.hotel.hotelservice.exception.NotFoundException;
import ru.hotel.hotelservice.mapper.HotelMapper;
import ru.hotel.hotelservice.repository.HotelRepository;
import ru.hotel.hotelservice.repository.RoomRepository;

@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final HotelMapper hotelMapper;

    @Override
    @Transactional
    public HotelResponse create(HotelRequest request) {
        return hotelMapper.toResponse(hotelRepository.save(hotelMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponse> findAll() {
        return hotelRepository.findAll().stream().map(hotelMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HotelResponse findById(Long id) {
        return hotelMapper.toResponse(getHotel(id));
    }

    @Override
    @Transactional
    public HotelResponse update(Long id, HotelRequest request) {
        Hotel hotel = getHotel(id);
        hotelMapper.update(hotel, request);
        return hotelMapper.toResponse(hotel);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!hotelRepository.existsById(id)) {
            throw notFound(id);
        }
        if (roomRepository.existsByHotelId(id)) {
            throw new ConflictException("Hotel " + id + " has rooms and cannot be deleted");
        }
        hotelRepository.deleteById(id);
    }

    private Hotel getHotel(Long id) {
        return hotelRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException("Hotel " + id + " not found");
    }
}
