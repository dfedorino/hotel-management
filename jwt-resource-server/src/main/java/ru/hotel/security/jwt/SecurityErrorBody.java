package ru.hotel.security.jwt;

import java.time.OffsetDateTime;

/** 401/403 body; same JSON shape as the services' ErrorResponse so clients parse one error format. */
public record SecurityErrorBody(OffsetDateTime timestamp, int status, String error, String message, String path) {}
