package ru.hotel.hotelservice.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import ru.hotel.hotelapi.api.HotelsApi;
import ru.hotel.hotelapi.model.HotelRequest;
import ru.hotel.hotelapi.model.HotelResponse;
import ru.hotel.hotelservice.service.HotelService;

@RestController
@RequiredArgsConstructor
public class HotelController implements HotelsApi {

    private final HotelService hotelService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public HotelResponse createHotel(HotelRequest request) {
        return hotelService.create(request);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<HotelResponse> listHotels() {
        return hotelService.findAll();
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public HotelResponse getHotel(Long id) {
        return hotelService.findById(id);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public HotelResponse updateHotel(Long id, HotelRequest request) {
        return hotelService.update(id, request);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteHotel(Long id) {
        hotelService.delete(id);
    }
}
