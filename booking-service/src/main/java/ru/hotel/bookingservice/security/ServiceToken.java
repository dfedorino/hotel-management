package ru.hotel.bookingservice.security;

import java.time.Instant;

/** Signed service token with its expiry, so callers can reuse it until shortly before it runs out. */
public record ServiceToken(String value, Instant expiresAt) {}
