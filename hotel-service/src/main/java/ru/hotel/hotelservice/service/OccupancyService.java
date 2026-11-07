package ru.hotel.hotelservice.service;

import ru.hotel.hotelapi.model.OccupancyReport;
import ru.hotel.hotelservice.dto.OccupancyQuery;

public interface OccupancyService {

    OccupancyReport report(OccupancyQuery query);
}
