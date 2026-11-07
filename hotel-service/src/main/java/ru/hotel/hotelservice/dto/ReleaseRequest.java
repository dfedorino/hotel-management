package ru.hotel.hotelservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Compensation request; {@code requestId} is the one used to confirm the slot.
 */
public record ReleaseRequest(@NotBlank String requestId) {
}
