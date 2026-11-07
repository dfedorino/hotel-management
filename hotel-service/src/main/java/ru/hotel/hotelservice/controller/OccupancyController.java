package ru.hotel.hotelservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import ru.hotel.hotelservice.dto.OccupancyQuery;
import ru.hotel.hotelservice.dto.OccupancyReport;
import ru.hotel.hotelservice.service.OccupancyService;

@RestController
@RequiredArgsConstructor
public class OccupancyController {

    private final OccupancyService occupancyService;

    @GetMapping("/api/rooms/occupancy")
    public OccupancyReport report(@Valid @ModelAttribute OccupancyQuery query) {
        return occupancyService.report(query);
    }
}
