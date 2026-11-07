package ru.hotel.hotelservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Room data from an administrator; a missing {@code available} flag means the room is in service.
 */
public record RoomRequest(
        @NotNull Long hotelId,
        @NotBlank @Size(max = 32) String number,
        Boolean available) {
}
