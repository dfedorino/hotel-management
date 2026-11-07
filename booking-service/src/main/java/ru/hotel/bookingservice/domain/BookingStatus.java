package ru.hotel.bookingservice.domain;

/** PENDING until Hotel Service holds the slot, then CONFIRMED; CANCELLED on failure, compensation or by the user. */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
