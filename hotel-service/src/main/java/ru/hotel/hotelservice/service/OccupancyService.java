package ru.hotel.hotelservice.service;

import ru.hotel.hotelservice.dto.OccupancyQuery;
import ru.hotel.hotelapi.model.OccupancyReport;

public interface OccupancyService {

    OccupancyReport report(OccupancyQuery query);
}
