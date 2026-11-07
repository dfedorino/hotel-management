package ru.hotel.hotelservice.controller;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import ru.hotel.hotelapi.api.OccupancyApi;
import ru.hotel.hotelapi.model.OccupancyReport;
import ru.hotel.hotelservice.dto.OccupancyQuery;
import ru.hotel.hotelservice.service.OccupancyService;

@RestController
@RequiredArgsConstructor
public class OccupancyController implements OccupancyApi {

    private final OccupancyService occupancyService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OccupancyReport> getOccupancy(LocalDate startDate, LocalDate endDate, Long hotelId) {
        return ResponseEntity.ok(occupancyService.report(new OccupancyQuery(hotelId, startDate, endDate)));
    }
}
