package ru.hotel.hotelservice.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<HotelResponse> createHotel(HotelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelService.create(request));
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<HotelResponse>> listHotels() {
        return ResponseEntity.ok(hotelService.findAll());
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<HotelResponse> getHotel(Long id) {
        return ResponseEntity.ok(hotelService.findById(id));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HotelResponse> updateHotel(Long id, HotelRequest request) {
        return ResponseEntity.ok(hotelService.update(id, request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHotel(Long id) {
        hotelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
