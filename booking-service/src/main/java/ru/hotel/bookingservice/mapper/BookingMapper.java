package ru.hotel.bookingservice.mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import ru.hotel.bookingapi.model.BookingResponse;
import ru.hotel.bookingservice.domain.Booking;

@Mapper
public interface BookingMapper {

    BookingResponse toResponse(Booking booking);

    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
