package ru.hotel.hotelservice.service;

import ru.hotel.hotelservice.dto.OccupancyQuery;
import ru.hotel.hotelservice.dto.OccupancyReport;

public interface OccupancyService {

    OccupancyReport report(OccupancyQuery query);
}
