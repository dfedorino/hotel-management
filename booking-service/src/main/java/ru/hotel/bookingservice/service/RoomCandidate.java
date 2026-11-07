package ru.hotel.bookingservice.service;

/** Free room offered by Hotel Service with its booking statistics. */
public record RoomCandidate(Long id, Long hotelId, String number, int timesBooked) {}
