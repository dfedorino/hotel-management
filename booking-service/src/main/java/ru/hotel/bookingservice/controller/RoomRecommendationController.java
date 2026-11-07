package ru.hotel.bookingservice.controller;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import ru.hotel.bookingapi.api.RoomsApi;
import ru.hotel.bookingapi.model.RecommendedRoom;
import ru.hotel.bookingservice.service.RoomRecommendationService;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class RoomRecommendationController implements RoomsApi {

    private final RoomRecommendationService recommendations;

    @Override
    public List<RecommendedRoom> recommendRooms(LocalDate startDate, LocalDate endDate, Long hotelId) {
        return recommendations.recommend(hotelId, startDate, endDate);
    }
}
